package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags private functions whose name appears nowhere else in the file. Like UnusedPrivateField,
 * it counts name occurrences, so overloads and uses in string templates count as uses.
 * Annotated functions are skipped, since frameworks may call them.
 */
public class UnusedPrivateFunctionCheck extends TreeCheck {

  @Override
  public void enterFunctionDefn(GosuParser.FunctionDefnContext ctx) {
    GosuParser.ModifiersContext modifiers = modifiersOf(ctx);
    if (!hasModifier(modifiers, "private") || hasAnnotation(modifiers)) {
      return;
    }
    String name = ctx.id().getText();
    if (file().nameOccurrences(name) <= 1) {
      report(ctx, "Remove this unused private function \"" + name + "\".");
    }
  }
}
