package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags functions and property accessors with an empty body and no explaining comment.
 * Constructors are skipped: an empty private constructor is a common way to prevent instantiation.
 */
public class EmptyFunctionCheck extends TreeCheck {

  @Override
  public void enterFunctionBody(GosuParser.FunctionBodyContext ctx) {
    ParserRuleContext parent = ctx.getParent();
    int index = parent.children.indexOf(ctx);
    if (index == 0) {
      return;
    }
    var declaration = parent.getChild(index - 1);
    boolean function = declaration instanceof GosuParser.FunctionDefnContext
      || declaration instanceof GosuParser.PropertyDefnContext;
    if (function && isEmptyWithoutComment(ctx.statementBlock())) {
      report((ParserRuleContext) declaration,
        "Add a comment explaining why this function is empty, or complete the implementation.");
    }
  }
}
