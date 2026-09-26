package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags calls to Gosu's global {@code print(...)} and uses of {@code System.out}/{@code System.err},
 * which bypass the application logger.
 */
public class PrintStatementCheck extends TreeCheck {

  @Override
  public void enterPrimaryExpr(GosuParser.PrimaryExprContext ctx) {
    check(ctx, ctx.typeLiteralExpr(), ctx.indirectMemberAccess());
  }

  @Override
  public void enterAssignmentOrMethodCall(GosuParser.AssignmentOrMethodCallContext ctx) {
    check(ctx, ctx.typeLiteralExpr(), ctx.indirectMemberAccess());
  }

  private void check(ParserRuleContext node, GosuParser.TypeLiteralExprContext target, GosuParser.IndirectMemberAccessContext access) {
    if (target == null) {
      return;
    }
    String name = target.getText();
    boolean isCall = access.getChildCount() > 0 && access.getChild(0) instanceof GosuParser.ArgumentsContext;
    if (isCall && name.equals("print")) {
      report(node, "Replace this \"print\" call with a logger.");
    } else if (name.equals("System.out") || name.startsWith("System.out.")) {
      report(node, "Replace this use of System.out with a logger.");
    } else if (name.equals("System.err") || name.startsWith("System.err.")) {
      report(node, "Replace this use of System.err with a logger.");
    }
  }
}
