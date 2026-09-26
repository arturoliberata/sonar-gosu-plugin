package org.sonargosu.plugin.parser;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Parse tree of one file plus any syntax errors. The tree is always present; when there are
 * errors, ANTLR's error recovery has filled it in as best it could.
 */
public record GosuParseResult(ParserRuleContext tree, List<SyntaxError> errors) {

  public record SyntaxError(int line, int column, String message) {
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
