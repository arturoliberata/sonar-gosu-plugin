package org.sonargosu.plugin.checks;

import java.util.List;
import java.util.Locale;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.lexer.GosuLexer;
import org.sonargosu.plugin.lexer.Token;
import org.sonargosu.plugin.parser.GosuParseResult;
import org.sonargosu.plugin.parser.GosuParserFacade;

/**
 * Everything a check needs to know about one source file: its lines, its tokens and,
 * when the file parsed cleanly, its parse tree.
 */
public final class GosuFile {

  private final String[] lines;
  private final List<Token> tokens;
  private final List<Token> codeTokens;
  private final List<GosuParseResult.SyntaxError> syntaxErrors;
  private final ParserRuleContext tree;

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
}
