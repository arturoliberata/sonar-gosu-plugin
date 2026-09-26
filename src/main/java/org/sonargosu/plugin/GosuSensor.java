package org.sonargosu.plugin;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonargosu.plugin.checks.GosuCheck;
import org.sonargosu.plugin.checks.GosuFile;
import org.sonargosu.plugin.lexer.Token;
import org.sonargosu.plugin.parser.GosuParseResult;
import org.sonargosu.plugin.rules.GosuRule;
import org.sonargosu.plugin.rules.GosuRulesDefinition;
import org.sonargosu.plugin.visitors.MetricsVisitor;
import org.sonargosu.plugin.visitors.SymbolTableVisitor;

/**
 * Analyzes every Gosu file: syntax highlighting, copy-paste detection tokens, metrics,
 * symbol table, executable lines, and issues from the active rules.
 */
public class GosuSensor implements Sensor {

  private static final Logger LOG = Loggers.get(GosuSensor.class);

  private final FileLinesContextFactory fileLinesContextFactory;

  public GosuSensor(FileLinesContextFactory fileLinesContextFactory) {
    this.fileLinesContextFactory = fileLinesContextFactory;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Gosu Sensor").onlyOnLanguage(GosuLanguage.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    Map<GosuRule, GosuCheck> checks = activeChecks(context);
    FileSystem fs = context.fileSystem();
    Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasLanguage(GosuLanguage.KEY));
    for (InputFile inputFile : files) {
      try {
        analyze(context, inputFile, checks);
      } catch (IOException | RuntimeException e) {
        LOG.warn("Unable to analyze Gosu file {}: {}", inputFile, e.getMessage());
        LOG.debug("Analysis failure", e);
      }
    }
  }

  private static Map<GosuRule, GosuCheck> activeChecks(SensorContext context) {
    Map<GosuRule, GosuCheck> checks = new EnumMap<>(GosuRule.class);
    for (GosuRule rule : GosuRule.values()) {
      ActiveRule activeRule = context.activeRules().find(RuleKey.of(GosuRulesDefinition.REPOSITORY_KEY, rule.key()));
      if (activeRule == null) {
        continue;
      }
      Map<String, String> params = new HashMap<>();
      rule.params().forEach(p -> params.put(p.key(), p.defaultValue()));
      params.putAll(activeRule.params());
      GosuCheck check = rule.newCheck();
      check.configure(params);
      checks.put(rule, check);
    }
    return checks;
  }

  private void analyze(SensorContext context, InputFile inputFile, Map<GosuRule, GosuCheck> checks) throws IOException {
    GosuFile file = new GosuFile(inputFile.contents(), inputFile.filename());
    if (!file.syntaxErrors().isEmpty()) {
      GosuParseResult.SyntaxError error = file.syntaxErrors().get(0);
      LOG.warn("Unable to parse file {} at line {}: {}", inputFile, error.line(), error.message());
    }
    saveHighlighting(context, inputFile, file.tokens());
    saveCpdTokens(context, inputFile, file.codeTokens());
    saveLineMetrics(context, inputFile, file);
    ParserRuleContext tree = file.tree();
    if (tree != null) {
      saveTreeMetrics(context, inputFile, MetricsVisitor.visit(tree));
      saveSymbolTable(context, inputFile, file.symbols());
    }
    for (Map.Entry<GosuRule, GosuCheck> entry : checks.entrySet()) {
      RuleKey ruleKey = RuleKey.of(GosuRulesDefinition.REPOSITORY_KEY, entry.getKey().key());
      entry.getValue().scan(file, (line, message) -> saveIssue(context, inputFile, ruleKey, line, message));
    }
  }

  private static void saveHighlighting(SensorContext context, InputFile inputFile, List<Token> tokens) {
    NewHighlighting highlighting = context.newHighlighting().onFile(inputFile);
    for (Token token : tokens) {
      TypeOfText type = switch (token.type()) {
        case KEYWORD -> TypeOfText.KEYWORD;
        case STRING -> TypeOfText.STRING;
        case NUMBER -> TypeOfText.CONSTANT;
        case ANNOTATION -> TypeOfText.ANNOTATION;
        case COMMENT -> TypeOfText.COMMENT;
        case DOC_COMMENT -> TypeOfText.STRUCTURED_COMMENT;
        default -> null;
      };
      if (type != null) {
        highlighting.highlight(range(inputFile, token), type);
      }
    }
    highlighting.save();
  }

  private static void saveCpdTokens(SensorContext context, InputFile inputFile, List<Token> tokens) {
    NewCpdTokens cpd = context.newCpdTokens().onFile(inputFile);
    int skipLine = -1;
    for (Token token : tokens) {
      // "uses" and "package" headers are legitimately repeated across files
      if (token.isKeyword("uses") || token.isKeyword("package")) {
        skipLine = token.line();
      }
      if (token.line() != skipLine) {
        cpd.addToken(range(inputFile, token), token.text());
      }
    }
    cpd.save();
  }

  /** Lines of code and comment lines: computed from tokens, so available even when parsing fails. */
  private static void saveLineMetrics(SensorContext context, InputFile inputFile, GosuFile file) {
    Set<Integer> codeLines = new TreeSet<>();
    Set<Integer> commentLines = new TreeSet<>();
    for (Token token : file.codeTokens()) {
      for (int line = token.line(); line <= token.endLine(); line++) {
        codeLines.add(line);
      }
    }
    for (Token token : file.tokens()) {
      if (token.type().isComment()) {
        String[] lines = token.text().split("\r\n|\r|\n", -1);
        for (int i = 0; i < lines.length; i++) {
          if (lines[i].replaceAll("[/*\\s]", "").length() > 0) {
            commentLines.add(token.line() + i);
          }
        }
      }
    }
    saveMeasure(context, inputFile, CoreMetrics.NCLOC, codeLines.size());
    saveMeasure(context, inputFile, CoreMetrics.COMMENT_LINES, commentLines.size());
  }

  private void saveTreeMetrics(SensorContext context, InputFile inputFile, MetricsVisitor metrics) {
    saveMeasure(context, inputFile, CoreMetrics.FUNCTIONS, metrics.functions());
    saveMeasure(context, inputFile, CoreMetrics.CLASSES, metrics.classes());
    saveMeasure(context, inputFile, CoreMetrics.STATEMENTS, metrics.statements());
    saveMeasure(context, inputFile, CoreMetrics.COMPLEXITY, metrics.complexity());

    FileLinesContext fileLines = fileLinesContextFactory.createFor(inputFile);
    for (int line : metrics.executableLines()) {
      fileLines.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1);
    }
    fileLines.save();
  }

  private static void saveSymbolTable(SensorContext context, InputFile inputFile, List<SymbolTableVisitor.Symbol> symbols) {
    NewSymbolTable table = context.newSymbolTable().onFile(inputFile);
    for (SymbolTableVisitor.Symbol symbol : symbols) {
      NewSymbol newSymbol = table.newSymbol(range(inputFile, symbol.declaration()));
      for (org.antlr.v4.runtime.Token reference : symbol.references()) {
        newSymbol.newReference(range(inputFile, reference));
      }
    }
    table.save();
  }

  private static void saveMeasure(SensorContext context, InputFile inputFile, Metric<Integer> metric, int value) {
    context.<Integer>newMeasure().on(inputFile).forMetric(metric).withValue(value).save();
  }

  private static void saveIssue(SensorContext context, InputFile inputFile, RuleKey ruleKey, int line, String message) {
    int safeLine = Math.max(1, Math.min(line, inputFile.lines()));
    NewIssue issue = context.newIssue().forRule(ruleKey);
    issue.at(issue.newLocation().on(inputFile).at(inputFile.selectLine(safeLine)).message(message)).save();
  }

  private static TextRange range(InputFile inputFile, Token token) {
    return inputFile.newRange(token.line(), token.column(), token.endLine(), token.endColumn());
  }

  /** Identifiers never span lines, so the end is on the start line. */
  private static TextRange range(InputFile inputFile, org.antlr.v4.runtime.Token token) {
    int column = token.getCharPositionInLine();
    return inputFile.newRange(token.getLine(), column, token.getLine(), column + token.getText().length());
  }
}
