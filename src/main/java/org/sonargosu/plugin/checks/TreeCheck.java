package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.sonargosu.plugin.parser.GosuBaseListener;

/**
 * Base class for checks that walk the parse tree. Override the {@code enterXxx}/{@code exitXxx}
 * methods of the generated listener and call {@link #report}. Files that do not parse are skipped;
 * the ParsingError rule reports those.
 */
public abstract class TreeCheck extends GosuBaseListener implements GosuCheck {

  private GosuFile file;
  private IssueCollector issues;

  @Override
  public final void scan(GosuFile file, IssueCollector issues) {
    if (file.tree() == null) {
      return;
    }
    this.file = file;
    this.issues = issues;
    ParseTreeWalker.DEFAULT.walk(this, file.tree());
  }

  protected GosuFile file() {
    return file;
  }

  protected void report(ParserRuleContext node, String message) {
    issues.report(node.getStart().getLine(), message);
  }
}
