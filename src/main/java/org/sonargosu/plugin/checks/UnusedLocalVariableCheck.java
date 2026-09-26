package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;
import org.sonargosu.plugin.visitors.SymbolTableVisitor;
import org.sonargosu.plugin.visitors.SymbolTableVisitor.Kind;

/**
 * Flags local variables that the symbol table links to no use. Names used in string templates
 * count as used, and resources of a "using" statement are skipped (using closes them).
 */
public class UnusedLocalVariableCheck implements GosuCheck {

  @Override
  public void scan(GosuFile file, IssueCollector issues) {
    for (SymbolTableVisitor.Symbol symbol : file.symbols()) {
      String name = symbol.declaration().getText();
      boolean unused = symbol.kind() == Kind.LOCAL_VARIABLE
        && symbol.references().isEmpty()
        && !(symbol.node().getParent() instanceof GosuParser.UsingStatementContext)
        && !file.templateIdentifiers().contains(name)
        && !name.equals("_");
      if (unused) {
        issues.report(symbol.declaration().getLine(), "Remove this unused \"" + name + "\" local variable.");
      }
    }
  }
}
