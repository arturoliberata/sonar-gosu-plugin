package org.sonargosu.plugin.checks;

import org.sonargosu.plugin.parser.GosuParser;

/**
 * Flags private fields whose name appears nowhere else in the file.
 *
 * Gosu fields are private unless declared public, protected or internal. Counting name occurrences
 * (instead of resolved references) also catches uses through {@code this._field} or
 * {@code other._field}; a name that happens to appear elsewhere only hides an issue, never
 * creates a false one. Fields exposed as properties ({@code as Name}) or carrying annotations
 * (frameworks may use them) are skipped.
 */
public class UnusedPrivateFieldCheck extends TreeCheck {

  @Override
  public void enterFieldDefn(GosuParser.FieldDefnContext ctx) {
    if (!(ctx.getParent() instanceof GosuParser.DeclarationContext)) {
      return; // interface fields are public constants
    }
    GosuParser.ModifiersContext modifiers = modifiersOf(ctx);
    boolean visible = hasModifier(modifiers, "public") || hasModifier(modifiers, "protected") || hasModifier(modifiers, "internal");
    boolean exposedAsProperty = ctx.id().size() > 1;
    if (visible || exposedAsProperty || hasAnnotation(modifiers)) {
      return;
    }
    String name = ctx.id(0).getText();
    if (file().nameOccurrences(name) <= 1) {
      report(ctx, "Remove this unused private field \"" + name + "\".");
    }
  }
}
