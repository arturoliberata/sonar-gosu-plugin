package org.sonargosu.plugin.checks;

import java.util.Map;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Cognitive Complexity (G. Ann Campbell, SonarSource) of each function, constructor and property:
 * <ul>
 *   <li>+1 for if, else if, else, switch, loops, catch and the ternary operator;</li>
 *   <li>+1 more per level of nesting for if, switch, loops, catch and ternary (not else / else if);</li>
 *   <li>+1 for each sequence of "and" / "or" operators;</li>
 *   <li>blocks ({@code \ x -> ...}) add a nesting level without an increment.</li>
 * </ul>
 * Anonymous classes are skipped here: their functions are measured on their own.
 */
public class CognitiveComplexityCheck extends TreeCheck {

  public static final String THRESHOLD_PARAM = "threshold";
  public static final int DEFAULT_THRESHOLD = 15;

  private int threshold = DEFAULT_THRESHOLD;

  @Override
  public void configure(Map<String, String> params) {
    threshold = Integer.parseInt(params.getOrDefault(THRESHOLD_PARAM, String.valueOf(DEFAULT_THRESHOLD)).trim());
  }

  @Override
  public void enterFunctionBody(GosuParser.FunctionBodyContext ctx) {
    ParserRuleContext parent = ctx.getParent();
    int index = parent.children.indexOf(ctx);
    if (index == 0 || !(parent.getChild(index - 1) instanceof ParserRuleContext declaration)) {
      return;
    }
    int complexity = complexity(ctx, 0);
    if (complexity > threshold) {
      report(declaration, "Refactor this function to reduce its Cognitive Complexity from " + complexity
        + " to the " + threshold + " allowed.");
    }
  }

  private static int complexity(ParseTree node, int nesting) {
    if (node instanceof GosuParser.IfStatementContext ifStatement) {
      return ifComplexity(ifStatement, nesting, false);
    }
    if (node instanceof GosuParser.WhileStatementContext
      || node instanceof GosuParser.DoWhileStatementContext
      || node instanceof GosuParser.ForEachStatementContext
      || node instanceof GosuParser.SwitchStatementContext
      || node instanceof GosuParser.CatchClauseContext) {
      return 1 + nesting + children(node, nesting + 1);
    }
    if (node instanceof GosuParser.ConditionalExprContext conditional && conditional.getChildCount() == 5) {
      return 1 + nesting + children(node, nesting + 1);
    }
    if (node instanceof GosuParser.ConditionalOrExprContext || node instanceof GosuParser.ConditionalAndExprContext) {
      int sequence = node.getChildCount() > 1 ? 1 : 0;
      return sequence + children(node, nesting);
    }
    if (node instanceof GosuParser.BlockExprContext) {
      return children(node, nesting + 1);
    }
    if (node instanceof GosuParser.AnonymousInnerClassContext) {
      return 0;
    }
    return children(node, nesting);
  }

  /** "else if" costs a flat +1 and stays at the same nesting level as its chain. */
  private static int ifComplexity(GosuParser.IfStatementContext ctx, int nesting, boolean elseIf) {
    int total = elseIf ? 1 : 1 + nesting;
    total += complexity(ctx.expression(), nesting);
    total += complexity(ctx.statement(0), nesting + 1);
    if (ctx.statement().size() > 1) {
      GosuParser.StatementContext elseBranch = ctx.statement(1);
      if (elseBranch.ifStatement() != null) {
        total += ifComplexity(elseBranch.ifStatement(), nesting, true);
      } else {
        total += 1 + complexity(elseBranch, nesting + 1);
      }
    }
    return total;
  }

  private static int children(ParseTree node, int nesting) {
    int total = 0;
    for (int i = 0; i < node.getChildCount(); i++) {
      total += complexity(node.getChild(i), nesting);
    }
    return total;
  }
}
