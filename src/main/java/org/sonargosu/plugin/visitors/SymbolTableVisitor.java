package org.sonargosu.plugin.visitors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.sonargosu.plugin.parser.GosuBaseListener;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Links each field, parameter and local variable to the places it is used, so SonarQube's code
 * viewer can highlight all occurrences when one is clicked.
 *
 * Resolution is purely lexical (no type information): a simple name used in an expression refers
 * to the innermost visible declaration with that name. Fields are visible in their whole class;
 * locals only after their declaration.
 */
public final class SymbolTableVisitor extends GosuBaseListener {

  public enum Kind {
    FIELD, PARAMETER, LOCAL_VARIABLE, LOOP_VARIABLE, CATCH_VARIABLE, BLOCK_PARAMETER
  }

  /**
   * A declaration and every reference to it. {@code node} is the declaring construct,
   * e.g. the LocalVarStatementContext of a local variable.
   */
  public record Symbol(Token declaration, Kind kind, ParserRuleContext node, List<Token> references) {
  }

  private final List<Symbol> symbols = new ArrayList<>();
  private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();
  private List<GosuParser.ParameterDeclarationContext> pendingParameters = new ArrayList<>();

  public static List<Symbol> visit(ParserRuleContext tree) {
    SymbolTableVisitor visitor = new SymbolTableVisitor();
    visitor.scopes.push(new HashMap<>());
    ParseTreeWalker.DEFAULT.walk(visitor, tree);
    return visitor.symbols;
  }

  private void declare(ParserRuleContext id, Kind kind, ParserRuleContext node) {
    if (id == null || scopes.isEmpty()) {
      return;
    }
    Token token = id.getStart();
    Symbol symbol = new Symbol(token, kind, node, new ArrayList<>());
    symbols.add(symbol);
    scopes.peek().put(token.getText(), symbol);
  }

  private void pushScope() {
    scopes.push(new HashMap<>());
  }

  private void popScope() {
    scopes.pop();
  }

  // --- class-level scopes: fields are declared up front so they resolve anywhere in the class ---

  @Override
  public void enterClassMembers(GosuParser.ClassMembersContext ctx) {
    pushScope();
    for (GosuParser.DeclarationContext declaration : ctx.declaration()) {
      if (declaration.fieldDefn() != null) {
        declare(declaration.fieldDefn().id(0), Kind.FIELD, declaration);
      }
    }
  }

  @Override
  public void exitClassMembers(GosuParser.ClassMembersContext ctx) {
    popScope();
  }

  @Override
  public void enterInterfaceMembers(GosuParser.InterfaceMembersContext ctx) {
    pushScope();
    for (GosuParser.FieldDefnContext field : ctx.fieldDefn()) {
      declare(field.id(0), Kind.FIELD, field);
    }
  }

  @Override
  public void exitInterfaceMembers(GosuParser.InterfaceMembersContext ctx) {
    popScope();
  }

  // --- functions: parameters are collected, then declared inside the body's scope ---

  @Override
  public void enterFunctionDefn(GosuParser.FunctionDefnContext ctx) {
    pendingParameters = new ArrayList<>();
  }

  @Override
  public void enterConstructorDefn(GosuParser.ConstructorDefnContext ctx) {
    pendingParameters = new ArrayList<>();
  }

  @Override
  public void enterPropertyDefn(GosuParser.PropertyDefnContext ctx) {
    pendingParameters = new ArrayList<>();
  }

  @Override
  public void enterParameterDeclaration(GosuParser.ParameterDeclarationContext ctx) {
    boolean isFunctionParameter = ctx.getParent().getParent() instanceof GosuParser.ParametersContext;
    if (isFunctionParameter) {
      pendingParameters.add(ctx);
    } else {
      declare(ctx.id(), Kind.BLOCK_PARAMETER, ctx); // block expression parameter: its scope is already open
    }
  }

  @Override
  public void enterFunctionBody(GosuParser.FunctionBodyContext ctx) {
    pushScope();
    for (GosuParser.ParameterDeclarationContext parameter : pendingParameters) {
      declare(parameter.id(), Kind.PARAMETER, parameter);
    }
    pendingParameters = new ArrayList<>();
  }

  @Override
  public void exitFunctionBody(GosuParser.FunctionBodyContext ctx) {
    popScope();
  }

  // --- nested scopes ---

  @Override
  public void enterStatementBlockBody(GosuParser.StatementBlockBodyContext ctx) {
    pushScope();
  }

  @Override
  public void exitStatementBlockBody(GosuParser.StatementBlockBodyContext ctx) {
    popScope();
  }

  @Override
  public void enterBlockExpr(GosuParser.BlockExprContext ctx) {
    pushScope();
  }

  @Override
  public void exitBlockExpr(GosuParser.BlockExprContext ctx) {
    popScope();
  }

  @Override
  public void enterForEachStatement(GosuParser.ForEachStatementContext ctx) {
    pushScope();
    declare(ctx.id(), Kind.LOOP_VARIABLE, ctx);
    GosuParser.IndexVarContext index = ctx.indexVar();
    if (index == null && ctx.indexRest() != null) {
      index = ctx.indexRest().indexVar();
    }
    if (index != null) {
      declare(index.id(), Kind.LOOP_VARIABLE, ctx);
    }
    if (ctx.indexRest() != null && ctx.indexRest().iteratorVar() != null) {
      declare(ctx.indexRest().iteratorVar().id(), Kind.LOOP_VARIABLE, ctx);
    }
  }

  @Override
  public void exitForEachStatement(GosuParser.ForEachStatementContext ctx) {
    popScope();
  }

  @Override
  public void enterCatchClause(GosuParser.CatchClauseContext ctx) {
    pushScope();
    declare(ctx.id(), Kind.CATCH_VARIABLE, ctx);
  }

  @Override
  public void exitCatchClause(GosuParser.CatchClauseContext ctx) {
    popScope();
  }

  // Declared on exit so that "var x = x + 1" refers to an outer x.
  @Override
  public void exitLocalVarStatement(GosuParser.LocalVarStatementContext ctx) {
    declare(ctx.id(), Kind.LOCAL_VARIABLE, ctx);
  }

  // --- references: the first name of a type literal used as an expression ("x", "x.foo()") ---

  @Override
  public void enterTypeLiteralExpr(GosuParser.TypeLiteralExprContext ctx) {
    GosuParser.TypeContext type = ctx.typeLiteral().type(0);
    if (type.classOrInterfaceType() == null) {
      return;
    }
    Token name = type.classOrInterfaceType().idclassOrInterfaceType().getStart();
    for (Map<String, Symbol> scope : scopes) {
      Symbol symbol = scope.get(name.getText());
      if (symbol != null) {
        symbol.references().add(name);
        return;
      }
    }
  }
}
