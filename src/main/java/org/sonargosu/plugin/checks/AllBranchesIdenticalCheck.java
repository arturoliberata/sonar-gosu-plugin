package org.sonargosu.plugin.checks;

import java.util.List;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags if/else chains, switch statements (with a default) and ternaries whose branches all do
 * the same thing: the condition makes no difference. Without a final else or default, identical
 * branches still differ from "do nothing", so those are left to DuplicateBranch.
 */
public class AllBranchesIdenticalCheck extends TreeCheck {

  private static final String MESSAGE = "Remove this conditional structure or edit its code blocks so that they're not all the same.";

  @Override
  public void enterIfStatement(GosuParser.IfStatementContext ctx) {
    if (!Branches.isChainHead(ctx)) {
      return;
    }
    Branches.IfChain chain = Branches.chain(ctx);
    if (chain.hasElse() && allSame(chain.branches().stream().map(Branches::statements).toList())) {
      report(ctx, MESSAGE);
    }
  }

  @Override
  public void enterSwitchStatement(GosuParser.SwitchStatementContext ctx) {
    List<GosuParser.SwitchBlockStatementGroupContext> groups = ctx.switchBlockStatementGroup();
    boolean hasDefault = groups.stream().anyMatch(g -> g.getStart().getText().equals("default"));
    if (hasDefault && groups.size() > 1 && allSame(groups.stream().map(Branches::statements).toList())) {
      report(ctx, MESSAGE);
    }
  }

  @Override
  public void enterConditionalExpr(GosuParser.ConditionalExprContext ctx) {
    // a ? b : c  has children: conditionalOrExpr '?' conditionalExpr ':' conditionalExpr
    if (ctx.getChildCount() == 5 && ctx.getChild(2).getText().equals(ctx.getChild(4).getText())) {
      report(ctx, "This conditional operator returns the same value whether the condition is true or false.");
    }
  }

  private static boolean allSame(List<List<GosuParser.StatementContext>> bodies) {
    String first = Branches.text(bodies.get(0));
    return !first.isEmpty() && bodies.stream().allMatch(b -> Branches.text(b).equals(first));
  }
}
