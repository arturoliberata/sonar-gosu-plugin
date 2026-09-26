package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags {@code x = x} and {@code a.b = a.b}. Targets containing a call are skipped,
 * since calling twice may give different results.
 */
public class SelfAssignmentCheck extends TreeCheck {

  @Override
  public void enterAssignmentOrMethodCall(GosuParser.AssignmentOrMethodCallContext ctx) {
    if (ctx.assignmentOp() == null || !ctx.assignmentOp().getText().equals("=")) {
      return;
    }
    String target = ctx.getChild(0).getText() + ctx.indirectMemberAccess().getText();
    if (!target.contains("(") && target.equals(ctx.expression().getText())) {
      report(ctx, "Remove this self-assignment of \"" + target + "\", or assign the intended value.");
    }
  }
}
