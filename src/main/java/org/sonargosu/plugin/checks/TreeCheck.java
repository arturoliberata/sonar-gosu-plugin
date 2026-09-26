package org.sonargosu.plugin.checks;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sonargosu.plugin.lexer.Token;
import org.sonargosu.plugin.parser.GosuBaseListener;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Base class for checks that walk the parse tree. Override the {@code enterXxx}/{@code exitXxx}
 * methods of the generated listener and call {@link #report}. Files that do not parse are skipped;
 * the ParsingError rule reports those.
 */
public abstract class TreeCheck extends GosuBaseListener implements GosuCheck {

  private GosuFile file;
  private IssueCollector issues;

  @Override
  public final void scan(GosuFile file, IssueCollector issues) {
    if (file.tree() == null) {
      return;
    }
    this.file = file;
    this.issues = issues;
    startFile();
    ParseTreeWalker.DEFAULT.walk(this, file.tree());
  }

  /** Called before each file is walked; override to reset per-file state. */
  protected void startFile() {
  }

  protected GosuFile file() {
    return file;
  }

  protected void report(ParserRuleContext node, String message) {
    issues.report(node.getStart().getLine(), message);
  }

  protected void report(TerminalNode token, String message) {
    issues.report(token.getSymbol().getLine(), message);
  }

  /**
   * The modifiers written before a declaration (function, property, field...), or null.
   * In the grammar they are the sibling immediately before the declaration.
   */
  protected static GosuParser.ModifiersContext modifiersOf(ParserRuleContext declaration) {
    ParserRuleContext parent = declaration.getParent();
    int index = parent.children.indexOf(declaration);
    return index > 0 && parent.getChild(index - 1) instanceof GosuParser.ModifiersContext modifiers ? modifiers : null;
  }

  protected static boolean hasModifier(GosuParser.ModifiersContext modifiers, String keyword) {
    if (modifiers == null) {
      return false;
    }
    for (int i = 0; i < modifiers.getChildCount(); i++) {
      if (modifiers.getChild(i).getText().equals(keyword)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean hasAnnotation(GosuParser.ModifiersContext modifiers) {
    return modifiers != null && !modifiers.annotation().isEmpty();
  }

  /** A block with no statements and no comment explaining why. */
  protected boolean isEmptyWithoutComment(GosuParser.StatementBlockContext block) {
    GosuParser.StatementBlockBodyContext body = block.statementBlockBody();
    return body.statement().isEmpty() && !containsComment(body);
  }

  /** True if a comment appears strictly between the first and last token of the node. */
  protected boolean containsComment(ParserRuleContext node) {
    int startLine = node.getStart().getLine();
    int startColumn = node.getStart().getCharPositionInLine();
    int stopLine = node.getStop().getLine();
    int stopColumn = node.getStop().getCharPositionInLine();
    for (Token token : file.tokens()) {
      if (token.type().isComment()
        && isAfter(token.line(), token.column(), startLine, startColumn)
        && isAfter(stopLine, stopColumn, token.line(), token.column())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAfter(int line, int column, int otherLine, int otherColumn) {
    return line > otherLine || (line == otherLine && column > otherColumn);
  }
}
