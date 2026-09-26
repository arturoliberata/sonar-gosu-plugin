package org.sonargosu.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonargosu.plugin.rules.GosuRule;
import org.sonargosu.plugin.rules.GosuRulesDefinition;

class GosuSensorTest {

  private static final String CODE = """
    package acme.claims

    uses java.util.List

    /** A sample class. */
    class ClaimHelper {
      // TODO remove
      function isBig(amount : int) : boolean {
        if (amount > 1000 and amount < 5000) {
          print("big")
          return true
        }
        return false
      }
    }
    """;

  @TempDir
  Path baseDir;

  private SensorContextTester context;
  private FileLinesContext fileLines;
  private GosuSensor sensor;

  @BeforeEach
  void setUp() {
    context = SensorContextTester.create(baseDir);
    fileLines = mock(FileLinesContext.class);
    FileLinesContextFactory factory = mock(FileLinesContextFactory.class);
    when(factory.createFor(any(InputFile.class))).thenReturn(fileLines);
    sensor = new GosuSensor(factory);
  }

  private void activateAllRules() {
    ActiveRulesBuilder rules = new ActiveRulesBuilder();
    for (GosuRule rule : GosuRule.values()) {
      rules.addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(GosuRulesDefinition.REPOSITORY_KEY, rule.key()))
        .build());
    }
    context.setActiveRules(rules.build());
  }

  private InputFile addFile(String path, String code) {
    InputFile file = TestInputFileBuilder.create("moduleKey", path)
      .setLanguage(GosuLanguage.KEY)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(code)
      .build();
    context.fileSystem().add(file);
    return file;
  }

  @Test
  void computes_metrics_highlighting_and_issues() {
    activateAllRules();
    InputFile file = addFile("src/ClaimHelper.gs", CODE);

    sensor.execute(context);

    assertThat(context.measure(file.key(), CoreMetrics.NCLOC).value()).isEqualTo(11);
    assertThat(context.measure(file.key(), CoreMetrics.COMMENT_LINES).value()).isEqualTo(2);
    assertThat(context.measure(file.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1);
    assertThat(context.measure(file.key(), CoreMetrics.CLASSES).value()).isEqualTo(1);
    // if, print, return true, return false
    assertThat(context.measure(file.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(4);
    // 1 (function) + 1 (if) + 1 (and)
    assertThat(context.measure(file.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(3);

    assertThat(context.highlightingTypeAt(file.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(context.highlightingTypeAt(file.key(), 5, 0)).containsExactly(TypeOfText.STRUCTURED_COMMENT);
    assertThat(context.highlightingTypeAt(file.key(), 10, 12)).containsExactly(TypeOfText.STRING);

    assertThat(context.cpdTokens(file.key())).isNotEmpty();

    assertThat(context.allIssues())
      .extracting(i -> i.ruleKey().rule(), i -> i.primaryLocation().textRange().start().line())
      .containsExactlyInAnyOrder(tuple("TodoComment", 7), tuple("PrintStatement", 10));
  }

  @Test
  void saves_executable_lines() {
    addFile("src/ClaimHelper.gs", CODE);

    sensor.execute(context);

    for (int line : new int[] {9, 10, 11, 13}) {
      verify(fileLines).setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1);
    }
    verify(fileLines, never()).setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, 8, 1);
    verify(fileLines).save();
  }

  @Test
  void saves_symbol_table() {
    InputFile file = addFile("src/ClaimHelper.gs", CODE);

    sensor.execute(context);

    // "amount" parameter declared on line 8 (column 17), used twice on line 9
    assertThat(context.referencesForSymbolAt(file.key(), 8, 17))
      .extracting(TextRange::start)
      .extracting(p -> p.line() + ":" + p.lineOffset())
      .containsExactlyInAnyOrder("9:8", "9:26");
  }

  @Test
  void unparsable_file_gets_parsing_error_and_line_metrics_only() {
    activateAllRules();
    InputFile file = addFile("src/Broken.gs", "class Broken {\n  function f( {\n  print(\"x\")\n}\n");

    sensor.execute(context);

    assertThat(context.allIssues()).extracting(i -> i.ruleKey().rule()).containsExactly("ParsingError");
    assertThat(context.measure(file.key(), CoreMetrics.NCLOC).value()).isEqualTo(4);
    assertThat(context.measure(file.key(), CoreMetrics.FUNCTIONS)).isNull();
    verify(fileLines, never()).setIntValue(eq(CoreMetrics.EXECUTABLE_LINES_DATA_KEY), anyInt(), anyInt());
  }

  @Test
  void does_not_report_inactive_rules() {
    addFile("src/A.gsp", "print(\"x\")");

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }
}
