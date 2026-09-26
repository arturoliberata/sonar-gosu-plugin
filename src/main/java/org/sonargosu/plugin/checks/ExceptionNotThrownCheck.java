package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags a statement that only creates an exception: {@code new IllegalStateException("...")}
 * without {@code throw}. The exception is discarded and the error goes unnoticed.
 */
public class ExceptionNotThrownCheck extends TreeCheck {

  @Override
  public void enterAssignmentOrMethodCall(GosuParser.AssignmentOrMethodCallContext ctx) {
    boolean onlyCreates = ctx.getChild(0) instanceof GosuParser.NewExprContext newExpr
      && newExpr.classOrInterfaceType() != null
      && ctx.indirectMemberAccess().getChildCount() == 0
      && ctx.assignmentOp() == null
      && ctx.incrementOp() == null;
    if (onlyCreates) {
      String type = ((GosuParser.NewExprContext) ctx.getChild(0)).classOrInterfaceType().getText();
      String name = type.substring(type.lastIndexOf('.') + 1);
      if (name.endsWith("Exception") || name.endsWith("Error") || name.equals("Throwable")) {
        report(ctx, "Throw this \"" + name + "\", or remove it: creating it without \"throw\" has no effect.");
      }
    }
  }
}
