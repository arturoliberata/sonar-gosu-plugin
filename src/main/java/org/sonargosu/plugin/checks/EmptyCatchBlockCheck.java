package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.lexer.Token;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags {@code catch (e : Exception) { }}. A catch block containing only a comment
 * is accepted, since the comment documents why the exception is ignored.
 */
public class EmptyCatchBlockCheck extends TreeCheck {

  @Override
  public void enterCatchClause(GosuParser.CatchClauseContext ctx) {
    GosuParser.StatementBlockBodyContext body = ctx.statementBlock().statementBlockBody();
    if (body.statement().isEmpty() && !containsComment(body)) {
      report(ctx, "Handle this exception or explain in a comment why it can be ignored.");
    }
  }

  private boolean containsComment(ParserRuleContext node) {
    int startLine = node.getStart().getLine();
    int startColumn = node.getStart().getCharPositionInLine();
    int stopLine = node.getStop().getLine();
    int stopColumn = node.getStop().getCharPositionInLine();
    for (Token token : file().tokens()) {
      if (token.type().isComment()
        && isAfter(token.line(), token.column(), startLine, startColumn)
        && isAfter(stopLine, stopColumn, token.line(), token.column())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAfter(int line, int column, int otherLine, int otherColumn) {
    return line > otherLine || (line == otherLine && column > otherColumn);
  }
}
