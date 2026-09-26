package org.sonargosu.plugin.checks;

import java.util.List;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags an if without else whose only content is another if without else.
 */
public class CollapsibleIfCheck extends TreeCheck {

  @Override
  public void enterIfStatement(GosuParser.IfStatementContext ctx) {
    if (hasElse(ctx)) {
      return;
    }
    GosuParser.IfStatementContext inner = onlyIf(ctx.statement(0));
    if (inner != null && !hasElse(inner)) {
      report(inner, "Merge this \"if\" statement with the enclosing one.");
    }
  }

  private static boolean hasElse(GosuParser.IfStatementContext ctx) {
    return ctx.statement().size() > 1;
  }

  /** The if statement that is the whole body, directly or as the single statement of a block. */
  private static GosuParser.IfStatementContext onlyIf(GosuParser.StatementContext body) {
    if (body.ifStatement() != null) {
      return body.ifStatement();
    }
    if (body.statementBlock() != null) {
      List<GosuParser.StatementContext> statements = body.statementBlock().statementBlockBody().statement();
      if (statements.size() == 1) {
        return statements.get(0).ifStatement();
      }
    }
    return null;
  }
}
