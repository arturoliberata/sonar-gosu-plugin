package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags {@code catch (e : Exception) { }}. A catch block containing only a comment
 * is accepted, since the comment documents why the exception is ignored.
 */
public class EmptyCatchBlockCheck extends TreeCheck {

  @Override
  public void enterCatchClause(GosuParser.CatchClauseContext ctx) {
    if (isEmptyWithoutComment(ctx.statementBlock())) {
      report(ctx, "Handle this exception or explain in a comment why it can be ignored.");
    }
  }
}
