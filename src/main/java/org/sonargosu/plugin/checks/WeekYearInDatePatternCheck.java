package org.sonargosu.plugin.checks;

import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags "Y" (week-based year) in date patterns that don't use week numbers ("w").
 * "YYYY-MM-dd" prints 2025-12-31 as 2026-12-31, because that day belongs to week 1 of 2026.
 *
 * Checked: the first argument of new SimpleDateFormat(...) and of ofPattern / applyPattern
 * calls (java.time DateTimeFormatter, SimpleDateFormat).
 */
public class WeekYearInDatePatternCheck extends TreeCheck {

  private static final Set<String> PATTERN_METHODS = Set.of("ofPattern", "applyPattern", "applyLocalizedPattern");

  @Override
  public void enterArguments(GosuParser.ArgumentsContext ctx) {
    if (ctx.argExpression().isEmpty() || !takesDatePattern(ctx)) {
      return;
    }
    String pattern = HardcodedValueCheck.stringValue(ctx.argExpression(0));
    if (pattern == null) {
      return;
    }
    String unquoted = pattern.replaceAll("'[^']*'", "");   // text between single quotes is literal text
    if (unquoted.contains("Y") && !unquoted.contains("w")) {
      report(ctx, "Make sure that week year \"Y\" is expected here instead of year \"y\" in \"" + pattern + "\".");
    }
  }

  private static boolean takesDatePattern(GosuParser.ArgumentsContext ctx) {
    ParserRuleContext parent = ctx.getParent();
    if (parent instanceof GosuParser.NewExprContext newExpr) {
      return newExpr.classOrInterfaceType() != null && newExpr.classOrInterfaceType().getText().endsWith("SimpleDateFormat");
    }
    return PATTERN_METHODS.contains(calledName(ctx));
  }

  /**
   * The name of the function these arguments are passed to: "x.ofPattern(...)" has
   * '.', ofPattern, typeArguments, arguments in the member access chain; "ofPattern(...)"
   * and "DateTimeFormatter.ofPattern(...)" are a type literal directly followed by the arguments.
   */
  static String calledName(GosuParser.ArgumentsContext ctx) {
    ParserRuleContext access = ctx.getParent();
    if (!(access instanceof GosuParser.IndirectMemberAccessContext)) {
      return "";
    }
    int index = access.children.indexOf(ctx);
    if (index >= 2 && access.getChild(index - 1) instanceof GosuParser.TypeArgumentsContext) {
      return access.getChild(index - 2).getText();
    }
    if (index == 0) {
      ParseTree target = access.getParent().getChild(0);
      String text = target.getText();
      return text.substring(text.lastIndexOf('.') + 1);
    }
    return "";
  }
}
