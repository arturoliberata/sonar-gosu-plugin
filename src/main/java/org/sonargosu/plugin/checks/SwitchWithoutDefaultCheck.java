package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

public class SwitchWithoutDefaultCheck extends TreeCheck {

  @Override
  public void enterSwitchStatement(GosuParser.SwitchStatementContext ctx) {
    boolean hasDefault = ctx.switchBlockStatementGroup().stream()
      .anyMatch(group -> group.getStart().getText().equals("default"));
    if (!hasDefault) {
      report(ctx, "Add a \"default\" case to this switch.");
    }
  }
}
