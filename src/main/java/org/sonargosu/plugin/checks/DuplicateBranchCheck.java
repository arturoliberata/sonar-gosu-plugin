package org.sonargosu.plugin.checks;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags a branch of an if chain or a switch case whose code duplicates an earlier branch.
 * Branches with a single statement are ignored (repeating "return null" is often clearer than
 * merging conditions), and so are structures where every branch is identical, including the
 * final else or default: AllBranchesIdentical reports those.
 */
public class DuplicateBranchCheck extends TreeCheck {

  @Override
  public void enterIfStatement(GosuParser.IfStatementContext ctx) {
    if (Branches.isChainHead(ctx)) {
      Branches.IfChain chain = Branches.chain(ctx);
      check(List.copyOf(chain.branches()), chain.branches().stream().map(Branches::statements).toList(), chain.hasElse());
    }
  }

  @Override
  public void enterSwitchStatement(GosuParser.SwitchStatementContext ctx) {
    List<GosuParser.SwitchBlockStatementGroupContext> groups = ctx.switchBlockStatementGroup();
    boolean hasDefault = groups.stream().anyMatch(g -> g.getStart().getText().equals("default"));
    check(List.copyOf(groups), groups.stream().map(Branches::statements).toList(), hasDefault);
  }

  private void check(List<ParserRuleContext> branches, List<List<GosuParser.StatementContext>> bodies, boolean exhaustive) {
    List<String> texts = bodies.stream().map(Branches::text).toList();
    if (exhaustive && texts.stream().distinct().count() == 1) {
      return;
    }
    for (int i = 1; i < bodies.size(); i++) {
      int first = texts.indexOf(texts.get(i));
      if (bodies.get(i).size() >= 2 && first < i) {
        report(branches.get(i), "This branch's code is identical to the branch on line "
          + branches.get(first).getStart().getLine() + ". Merge the conditions or change one of them.");
      }
    }
  }
}
