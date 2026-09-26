package org.sonargosu.plugin.visitors;

import java.util.Set;
import java.util.TreeSet;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonargosu.plugin.parser.GosuBaseListener;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Computes tree-based metrics for one file: functions, classes, statements, cyclomatic complexity
 * and executable lines (the lines a coverage report is expected to cover).
 */
public final class MetricsVisitor extends GosuBaseListener {

  private int functions;
  private int classes;
  private int statements;
  private int complexity;
  private final Set<Integer> executableLines = new TreeSet<>();

  public static MetricsVisitor visit(ParserRuleContext tree) {
    MetricsVisitor visitor = new MetricsVisitor();
    ParseTreeWalker.DEFAULT.walk(visitor, tree);
    return visitor;
  }

  public int functions() {
    return functions;
  }

  public int classes() {
    return classes;
  }

  public int statements() {
    return statements;
  }

  public int complexity() {
    return complexity;
  }

  public Set<Integer> executableLines() {
    return executableLines;
  }

  // --- functions: each one also starts a complexity path ---

  @Override
  public void enterFunctionDefn(GosuParser.FunctionDefnContext ctx) {
    functions++;
    complexity++;
  }

  @Override
  public void enterConstructorDefn(GosuParser.ConstructorDefnContext ctx) {
    functions++;
    complexity++;
  }

  @Override
  public void enterPropertyDefn(GosuParser.PropertyDefnContext ctx) {
    functions++;
    complexity++;
  }

  // --- classes ---

  @Override
  public void enterGClass(GosuParser.GClassContext ctx) {
    classes++;
  }

  @Override
  public void enterGInterfaceOrStructure(GosuParser.GInterfaceOrStructureContext ctx) {
    classes++;
  }

  @Override
  public void enterGEnum(GosuParser.GEnumContext ctx) {
    classes++;
  }

  @Override
  public void enterGEnhancement(GosuParser.GEnhancementContext ctx) {
    classes++;
  }

  @Override
  public void enterGAnnotation(GosuParser.GAnnotationContext ctx) {
    classes++;
  }

  // --- statements and executable lines ---

  @Override
  public void enterStatement(GosuParser.StatementContext ctx) {
    boolean isBlock = ctx.statementBlock() != null;
    boolean isEmpty = ctx.getChildCount() == 1 && ctx.getChild(0) instanceof TerminalNode;
    if (!isBlock && !isEmpty) {
      statements++;
      executableLines.add(ctx.getStart().getLine());
    }
  }

  // --- branches ---

  @Override
  public void enterIfStatement(GosuParser.IfStatementContext ctx) {
    complexity++;
  }

  @Override
  public void enterWhileStatement(GosuParser.WhileStatementContext ctx) {
    complexity++;
  }

  @Override
  public void enterDoWhileStatement(GosuParser.DoWhileStatementContext ctx) {
    complexity++;
  }

  @Override
  public void enterForEachStatement(GosuParser.ForEachStatementContext ctx) {
    complexity++;
  }

  @Override
  public void enterCatchClause(GosuParser.CatchClauseContext ctx) {
    complexity++;
  }

  @Override
  public void enterSwitchBlockStatementGroup(GosuParser.SwitchBlockStatementGroupContext ctx) {
    if (ctx.getStart().getText().equals("case")) {
      complexity++;
    }
  }

  @Override
  public void enterConditionalExpr(GosuParser.ConditionalExprContext ctx) {
    if (ctx.getChildCount() > 1) {
      complexity++; // a ? b : c   or   a ?: b
    }
  }

  @Override
  public void enterOrOp(GosuParser.OrOpContext ctx) {
    complexity++;
  }

  @Override
  public void enterAndOp(GosuParser.AndOpContext ctx) {
    complexity++;
  }
}
