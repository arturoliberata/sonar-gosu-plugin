package org.sonargosu.plugin.checks;

import java.util.regex.Pattern;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags {@code new BigDecimal(0.1)}: the double literal is converted with its binary rounding
 * error (0.1000000000000000055511151231257827...). Without type information only literal
 * arguments are detected, which covers the common case of hard-coded rates and amounts.
 */
public class BigDecimalFromDoubleCheck extends TreeCheck {

  // Floating point literals: 0.1, .5, 1e3, 2.5d, 3f (but not 1, 0.1bd or 1.5r)
  private static final Pattern FLOATING_LITERAL = Pattern.compile(
    "-?(?:(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?|\\d+[eE][+-]?\\d+)[dDfF]?|-?\\d+[dDfF]");

  @Override
  public void enterNewExpr(GosuParser.NewExprContext ctx) {
    if (ctx.classOrInterfaceType() == null || ctx.arguments() == null || ctx.arguments().argExpression().isEmpty()) {
      return;
    }
    String type = ctx.classOrInterfaceType().getText();
    String argument = ctx.arguments().argExpression(0).getText();
    if ((type.equals("BigDecimal") || type.endsWith(".BigDecimal")) && FLOATING_LITERAL.matcher(argument).matches()) {
      String digits = argument.replaceAll("[dDfF]$", "");
      report(ctx, "Use \"new BigDecimal(\\\"" + digits + "\\\")\", \"BigDecimal.valueOf(" + digits + ")\" or the "
        + digits + "bd literal instead of a double.");
    }
  }
}
