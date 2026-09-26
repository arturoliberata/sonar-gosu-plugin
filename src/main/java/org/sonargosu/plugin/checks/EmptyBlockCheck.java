package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags empty blocks in if/else/loops/try/finally/using statements, and bare nested blocks.
 * Catch blocks (EmptyCatchBlock), function bodies (EmptyFunction) and {@code \ -> {}} blocks
 * are left to other rules or are a normal idiom.
 */
public class EmptyBlockCheck extends TreeCheck {

  @Override
  public void enterStatementBlock(GosuParser.StatementBlockContext ctx) {
    ParserRuleContext parent = ctx.getParent();
    boolean statementBody = parent instanceof GosuParser.StatementContext
      || parent instanceof GosuParser.TryCatchFinallyStatementContext
      || parent instanceof GosuParser.UsingStatementContext;
    if (statementBody && isEmptyWithoutComment(ctx)) {
      report(ctx, "Either remove or fill this block of code, or add a comment explaining why it is empty.");
    }
  }
}
