package org.sonargosu.plugin.checks;

import java.util.HashSet;
import java.util.Set;
import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags a {@code uses a.b.C} statement that repeats an earlier one in the same file.
 */
public class DuplicateUsesCheck extends TreeCheck {

  private final Set<String> seen = new HashSet<>();

  @Override
  protected void startFile() {
    seen.clear();
  }

  @Override
  public void enterUsesStatement(GosuParser.UsesStatementContext ctx) {
    String name = ctx.getText().replace(";", "");
    if (!seen.add(name)) {
      report(ctx, "Remove this duplicated \"uses " + name + "\" statement.");
    }
  }
}
