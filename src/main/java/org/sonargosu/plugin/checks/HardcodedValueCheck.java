package org.sonargosu.plugin.checks;

import java.util.Locale;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Base for rules that look for a sensitive name (password, token...) given a string literal value.
 * It finds name/value pairs in: local variables, fields, assignments ({@code x.Password = "..."}),
 * named arguments ({@code :password = "..."}), object initializers and map initializers
 * ({@code "password" -> "..."}). Subclasses decide which names and values are suspicious, and can
 * also inspect every string literal through {@link #visitStringLiteral}.
 */
public abstract class HardcodedValueCheck extends TreeCheck {

  /** Lowercase, without "_", "-" and "." so that DB_PASSWORD, dbPassword and db.password compare equal. */
  protected static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT).replaceAll("[_.\\-]", "");
  }

  protected abstract boolean isSensitiveName(String normalizedName);

  protected abstract boolean isSensitiveValue(String normalizedName, String value);

  protected abstract String message(String name);

  /** Called for every string literal token, with its unquoted value. */
  protected void visitStringLiteral(TerminalNode literal, String value) {
  }

  @Override
  public void enterLocalVarStatement(GosuParser.LocalVarStatementContext ctx) {
    check(ctx.id().getText(), ctx.expression());
  }

  @Override
  public void enterFieldDefn(GosuParser.FieldDefnContext ctx) {
    check(ctx.id(0).getText(), ctx.expression());
  }

  @Override
  public void enterAssignmentOrMethodCall(GosuParser.AssignmentOrMethodCallContext ctx) {
    if (ctx.assignmentOp() != null && ctx.assignmentOp().getText().equals("=")) {
      String target = ctx.getChild(0).getText() + ctx.indirectMemberAccess().getText();
      check(target.substring(target.lastIndexOf('.') + 1), ctx.expression());
    }
  }

  @Override
  public void enterNamedArgumentExpression(GosuParser.NamedArgumentExpressionContext ctx) {
    check(ctx.idAll().getText(), ctx.expression());
  }

  @Override
  public void enterInitializerAssignment(GosuParser.InitializerAssignmentContext ctx) {
    check(ctx.idAll().getText(), ctx.expression());
  }

  @Override
  public void enterMapInitializerList(GosuParser.MapInitializerListContext ctx) {
    for (int i = 0; i + 1 < ctx.expression().size(); i += 2) {
      String key = stringValue(ctx.expression(i));
      if (key != null) {
        check(key, ctx.expression(i + 1));
      }
    }
  }

  @Override
  public void visitTerminal(TerminalNode node) {
    if (node.getSymbol().getType() == GosuParser.StringLiteral) {
      visitStringLiteral(node, unquote(node.getText()));
    }
  }

  private void check(String name, GosuParser.ExpressionContext value) {
    String literal = stringValue(value);
    String normalized = normalize(name);
    if (literal != null && isSensitiveName(normalized) && isSensitiveValue(normalized, literal)) {
      report(value, message(name));
    }
  }

  /** The value of an expression that is a single string literal without template, or null. */
  protected static String stringValue(ParserRuleContext expression) {
    if (expression == null || expression.getStart() != expression.getStop()
      || expression.getStart().getType() != GosuParser.StringLiteral) {
      return null;
    }
    String value = unquote(expression.getText());
    return value.contains("${") || value.contains("<%") ? null : value;
  }

  private static String unquote(String literal) {
    return literal.length() >= 2 ? literal.substring(1, literal.length() - 1) : literal;
  }
}
