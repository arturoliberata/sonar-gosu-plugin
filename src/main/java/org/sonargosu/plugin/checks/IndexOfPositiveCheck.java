package org.sonargosu.plugin.checks;

import java.util.regex.Pattern;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags {@code s.indexOf(x) > 0} (and {@code 0 < s.indexOf(x)}): a match at the very start,
 * index 0, is treated as "not found". Use {@code >= 0}, or {@code contains}.
 */
public class IndexOfPositiveCheck extends TreeCheck {

  private static final Pattern INDEX_OF_CALL = Pattern.compile(".*\\.(?:indexOf|lastIndexOf)\\(.*\\)");

  @Override
  public void enterRelationalExpr(GosuParser.RelationalExprContext ctx) {
    if (ctx.getChildCount() != 3 || !(ctx.getChild(1) instanceof GosuParser.RelOpContext)) {
      return;
    }
    String left = ctx.getChild(0).getText();
    String operator = ctx.getChild(1).getText();
    String right = ctx.getChild(2).getText();
    boolean indexGreaterThanZero = operator.equals(">") && right.equals("0") && INDEX_OF_CALL.matcher(left).matches();
    boolean zeroLessThanIndex = operator.equals("<") && left.equals("0") && INDEX_OF_CALL.matcher(right).matches();
    if (indexGreaterThanZero || zeroLessThanIndex) {
      report(ctx, "0 is a valid index, but this check treats it as \"not found\". Use \">= 0\", or \"contains\".");
    }
  }
}
