package org.sonargosu.plugin.checks;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.lexer.GosuLexer;
import org.sonargosu.plugin.lexer.Token;
import org.sonargosu.plugin.lexer.TokenType;
import org.sonargosu.plugin.parser.GosuParseResult;
import org.sonargosu.plugin.parser.GosuParserFacade;
import org.sonargosu.plugin.visitors.SymbolTableVisitor;

/**
 * Everything a check needs to know about one source file: its lines, its tokens and,
 * when the file parsed cleanly, its parse tree and symbol table.
 */
public final class GosuFile {

  private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*");

  private final String[] lines;
  private final List<Token> tokens;
  private final List<Token> codeTokens;
  private final List<GosuParseResult.SyntaxError> syntaxErrors;
  private final ParserRuleContext tree;
  private List<SymbolTableVisitor.Symbol> symbols;
  private Set<String> templateIdentifiers;

  /** For tests: parses the contents as a class file. */
  public GosuFile(String contents) {
    this(contents, "file.gs");
  }

  public GosuFile(String contents, String filename) {
    this.lines = contents.split("\r\n|\r|\n", -1);
    this.tokens = GosuLexer.tokenize(contents);
    this.codeTokens = tokens.stream().filter(t -> !t.type().isComment()).toList();
    if (filename.toLowerCase(Locale.ROOT).endsWith(".gst")) {
      // Templates mix text and code; the grammar does not cover them.
      this.syntaxErrors = List.of();
      this.tree = null;
    } else {
      GosuParseResult result = GosuParserFacade.parse(contents, GosuParserFacade.kindOf(filename));
      this.syntaxErrors = result.errors();
      this.tree = result.hasErrors() ? null : result.tree();
    }
  }

  /** Source lines, index 0 is line 1. */
  public String[] lines() {
    return lines;
  }

  /** All tokens, comments included. */
  public List<Token> tokens() {
    return tokens;
  }

  /** Tokens without comments. */
  public List<Token> codeTokens() {
    return codeTokens;
  }

  /** The parse tree, or null if the file has syntax errors or is a template. */
  public ParserRuleContext tree() {
    return tree;
  }

  public List<GosuParseResult.SyntaxError> syntaxErrors() {
    return syntaxErrors;
  }

  /** Declarations and their references; empty if the file did not parse. Computed once. */
  public List<SymbolTableVisitor.Symbol> symbols() {
    if (symbols == null) {
      symbols = tree == null ? List.of() : SymbolTableVisitor.visit(tree);
    }
    return symbols;
  }

  /**
   * Names used inside string templates ({@code "${total}"}, {@code "<%= name %>"}). The parse tree
   * sees a string literal as a single token, so these uses are invisible to it.
   */
  public Set<String> templateIdentifiers() {
    if (templateIdentifiers == null) {
      templateIdentifiers = new HashSet<>();
      for (Token token : codeTokens) {
        if (token.type() == TokenType.STRING) {
          collectTemplateIdentifiers(token.text(), templateIdentifiers);
        }
      }
    }
    return templateIdentifiers;
  }

  /** How often a name is used in this file: as a code token, or inside a string template. */
  public int nameOccurrences(String name) {
    int count = 0;
    for (Token token : codeTokens) {
      if (token.text().equals(name)) {
        count++;
      }
    }
    return templateIdentifiers().contains(name) ? count + 1 : count;
  }

  private static void collectTemplateIdentifiers(String literal, Set<String> names) {
    int i = 0;
    while (i < literal.length()) {
      int start;
      int end;
      if (literal.startsWith("${", i)) {
        start = i + 2;
        end = matchingBrace(literal, i + 1);
      } else if (literal.startsWith("<%", i)) {
        start = i + 2;
        end = literal.indexOf("%>", start);
        end = end < 0 ? literal.length() : end;
      } else {
        i++;
        continue;
      }
      Matcher matcher = IDENTIFIER.matcher(literal.substring(start, end));
      while (matcher.find()) {
        names.add(matcher.group());
      }
      i = end + 1;
    }
  }

  private static int matchingBrace(String text, int open) {
    int depth = 0;
    for (int i = open; i < text.length(); i++) {
      if (text.charAt(i) == '{') {
        depth++;
      } else if (text.charAt(i) == '}' && --depth == 0) {
        return i;
      }
    }
    return text.length();
  }
}
