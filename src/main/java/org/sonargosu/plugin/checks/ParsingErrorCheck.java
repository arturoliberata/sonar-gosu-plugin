package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParseResult;

/**
 * Reports the first syntax error of a file. Rules based on the parse tree cannot run on that file,
 * so this makes the gap visible instead of silently reporting fewer issues.
 */
public class ParsingErrorCheck implements GosuCheck {

  @Override
  public void scan(GosuFile file, IssueCollector issues) {
    if (!file.syntaxErrors().isEmpty()) {
      GosuParseResult.SyntaxError error = file.syntaxErrors().get(0);
      issues.report(error.line(), "Gosu parser failure: " + error.message());
    }
  }
}
