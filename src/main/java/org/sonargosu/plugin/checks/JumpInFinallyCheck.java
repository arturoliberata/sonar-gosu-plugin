package org.sonargosu.plugin.checks;

import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags return and throw inside a finally block, and break/continue that leave it.
 * Blocks ({@code \ -> ...}) and anonymous classes inside the finally block have their own
 * control flow and are not inspected.
 */
public class JumpInFinallyCheck extends TreeCheck {

  private final Set<ParserRuleContext> reported = new HashSet<>();

  @Override
  protected void startFile() {
    reported.clear();
  }

  @Override
  public void enterTryCatchFinallyStatement(GosuParser.TryCatchFinallyStatementContext ctx) {
    for (int i = 0; i + 1 < ctx.getChildCount(); i++) {
      if (ctx.getChild(i) instanceof TerminalNode terminal && terminal.getText().equals("finally")) {
        inspect(ctx.getChild(i + 1), false);
      }
    }
  }

  private void inspect(ParseTree node, boolean insideLoopOrSwitch) {
    if (node instanceof GosuParser.BlockExprContext || node instanceof GosuParser.AnonymousInnerClassContext) {
      return;
    }
    if (node instanceof GosuParser.ReturnStatementContext || node instanceof GosuParser.ThrowStatementContext) {
      flag((ParserRuleContext) node);
      return;
    }
    if (node instanceof GosuParser.StatementContext statement && !insideLoopOrSwitch) {
      String first = statement.getStart().getText();
      if ((first.equals("break") || first.equals("continue")) && statement.getChild(0) instanceof TerminalNode) {
        flag(statement);
        return;
      }
    }
    boolean loopOrSwitch = insideLoopOrSwitch
      || node instanceof GosuParser.WhileStatementContext
      || node instanceof GosuParser.DoWhileStatementContext
      || node instanceof GosuParser.ForEachStatementContext
      || node instanceof GosuParser.SwitchStatementContext;
    for (int i = 0; i < node.getChildCount(); i++) {
      inspect(node.getChild(i), loopOrSwitch);
    }
  }

  private void flag(ParserRuleContext node) {
    if (reported.add(node)) {
      report(node, "Remove this \"" + node.getStart().getText() + "\" from the \"finally\" block.");
    }
  }
}
