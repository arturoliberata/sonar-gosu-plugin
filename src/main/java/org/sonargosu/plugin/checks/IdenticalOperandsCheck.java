package org.sonargosu.plugin.checks;

import java.util.Set;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags the same expression on both sides of a comparison, logical, bitwise, subtraction,
 * division or remainder operator. Addition and multiplication are fine ({@code x * x}).
 *
 * Binary expressions are chains like {@code a - b - c} (operand, operator, operand, ...), evaluated
 * left to right, so the left operand of each operator is everything before it.
 */
public class IdenticalOperandsCheck extends TreeCheck {

  private static final Set<String> ARITHMETIC = Set.of("-", "?-", "!-", "/", "?/", "%", "?%");

  // Comparing two literals (5 == 5, true != true) is test scaffolding, not a copy-paste bug.
  private static final Pattern LITERAL = Pattern.compile("true|false|null|NaN|Infinity|[0-9.][\\w.]*|\".*\"|'.*'");

  @Override
  public void enterConditionalOrExpr(GosuParser.ConditionalOrExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterConditionalAndExpr(GosuParser.ConditionalAndExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterBitwiseOrExpr(GosuParser.BitwiseOrExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterBitwiseXorExpr(GosuParser.BitwiseXorExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterBitwiseAndExpr(GosuParser.BitwiseAndExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterEqualityExpr(GosuParser.EqualityExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterRelationalExpr(GosuParser.RelationalExprContext ctx) {
    check(ctx, false);
  }

  @Override
  public void enterAdditiveExpr(GosuParser.AdditiveExprContext ctx) {
    check(ctx, true);
  }

  @Override
  public void enterMultiplicativeExpr(GosuParser.MultiplicativeExprContext ctx) {
    check(ctx, true);
  }

  private void check(ParserRuleContext ctx, boolean arithmetic) {
    StringBuilder left = new StringBuilder();
    for (int i = 0; i + 1 < ctx.getChildCount(); i++) {
      ParseTree child = ctx.getChild(i);
      if (isOperator(child) && isChecked(child, arithmetic) && !isOperator(ctx.getChild(i + 1))) {
        String right = ctx.getChild(i + 1).getText();
        if (left.toString().equals(right) && !right.contains("(") && !LITERAL.matcher(right).matches()) {
          report(ctx, "Correct one of the identical sub-expressions on both sides of operator \"" + child.getText() + "\".");
        }
      }
      left.append(child.getText());
    }
  }

  // Operators are tokens ('|', '^', '&', 'typeis') or the xxxOp rules; operands are sub-expressions.
  // Binder expressions ("5 kg") put two operands side by side with no operator.
  private static boolean isOperator(ParseTree node) {
    return node instanceof TerminalNode
      || node instanceof GosuParser.OrOpContext
      || node instanceof GosuParser.AndOpContext
      || node instanceof GosuParser.EqualityOpContext
      || node instanceof GosuParser.RelOpContext
      || node instanceof GosuParser.AdditiveOpContext
      || node instanceof GosuParser.MultiplicativeOpContext;
  }

  private static boolean isChecked(ParseTree operator, boolean arithmetic) {
    if (operator.getText().equals("typeis")) {
      return false;
    }
    return !arithmetic || ARITHMETIC.contains(operator.getText());
  }
}
