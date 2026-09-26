package org.sonargosu.plugin.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Helpers for rules that compare the branches of if/else-if chains and switch statements.
 */
final class Branches {

  private Branches() {
  }

  /** An if/else-if/else chain: each if (in order), each branch body, and whether it ends with a plain else. */
  record IfChain(List<GosuParser.IfStatementContext> ifs, List<GosuParser.StatementContext> branches, boolean hasElse) {
  }

  /** False for the "if" of an "else if": the chain is handled once, from its first if. */
  static boolean isChainHead(GosuParser.IfStatementContext ctx) {
    ParserRuleContext parent = ctx.getParent();
    return !(parent instanceof GosuParser.StatementContext
      && parent.getParent() instanceof GosuParser.IfStatementContext outer
      && outer.statement().size() > 1
      && outer.statement(1) == parent);
  }

  static IfChain chain(GosuParser.IfStatementContext head) {
    List<GosuParser.IfStatementContext> ifs = new ArrayList<>();
    List<GosuParser.StatementContext> branches = new ArrayList<>();
    GosuParser.IfStatementContext current = head;
    while (true) {
      ifs.add(current);
      branches.add(current.statement(0));
      if (current.statement().size() < 2) {
        return new IfChain(ifs, branches, false);
      }
      GosuParser.StatementContext elseBranch = current.statement(1);
      if (elseBranch.ifStatement() == null) {
        branches.add(elseBranch);
        return new IfChain(ifs, branches, true);
      }
      current = elseBranch.ifStatement();
    }
  }

  /** The statements of a branch: the block's statements, or the single statement. */
  static List<GosuParser.StatementContext> statements(GosuParser.StatementContext branch) {
    if (branch.statementBlock() != null) {
      return branch.statementBlock().statementBlockBody().statement();
    }
    return List.of(branch);
  }

  /** The statements of a switch case group, without a trailing "break". */
  static List<GosuParser.StatementContext> statements(GosuParser.SwitchBlockStatementGroupContext group) {
    List<GosuParser.StatementContext> statements = group.statement();
    if (!statements.isEmpty() && text(statements.get(statements.size() - 1)).equals("break")) {
      return statements.subList(0, statements.size() - 1);
    }
    return statements;
  }

  /** Whitespace-free text of a list of statements, used to compare implementations. */
  static String text(List<GosuParser.StatementContext> statements) {
    return statements.stream().map(Branches::text).collect(Collectors.joining("\n"));
  }

  static String text(GosuParser.StatementContext statement) {
    String text = statement.getText();
    return text.endsWith(";") ? text.substring(0, text.length() - 1) : text;
  }
}
