package org.sonargosu.plugin.checks;

import java.util.HashMap;
import java.util.Map;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags a condition that repeats an earlier condition of the same if/else-if chain:
 * the second branch can never run.
 */
public class DuplicateConditionCheck extends TreeCheck {

  @Override
  public void enterIfStatement(GosuParser.IfStatementContext ctx) {
    if (!Branches.isChainHead(ctx)) {
      return;
    }
    Map<String, Integer> seen = new HashMap<>();
    for (GosuParser.IfStatementContext branch : Branches.chain(ctx).ifs()) {
      String condition = branch.expression().getText();
      Integer firstLine = seen.putIfAbsent(condition, branch.getStart().getLine());
      if (firstLine != null) {
        report(branch.expression(), "This condition duplicates the one on line " + firstLine
          + ", so this branch can never run.");
      }
    }
  }
}
