package org.sonargosu.plugin.checks;

import java.util.Map;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Counts parameters of function and constructor declarations.
 */
public class TooManyParametersCheck extends TreeCheck {

  public static final String MAX_PARAM = "max";
  public static final int DEFAULT_MAX = 7;

  private int max = DEFAULT_MAX;

  @Override
  public void configure(Map<String, String> params) {
    max = Integer.parseInt(params.getOrDefault(MAX_PARAM, String.valueOf(DEFAULT_MAX)).trim());
  }

  @Override
  public void enterFunctionDefn(GosuParser.FunctionDefnContext ctx) {
    check(ctx, ctx.parameters());
  }

  @Override
  public void enterConstructorDefn(GosuParser.ConstructorDefnContext ctx) {
    check(ctx, ctx.parameters());
  }

  private void check(ParserRuleContext declaration, GosuParser.ParametersContext parameters) {
    GosuParser.ParameterDeclarationListContext list = parameters.parameterDeclarationList();
    int count = list == null ? 0 : list.parameterDeclaration().size();
    if (count > max) {
      report(declaration, "This function has " + count + " parameters, which is greater than the " + max + " authorized.");
    }
  }
}
