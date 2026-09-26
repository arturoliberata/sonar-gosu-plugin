package org.sonargosu.plugin.rules;

import java.util.List;
import java.util.function.Supplier;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonargosu.plugin.checks.DuplicateUsesCheck;
import org.sonargosu.plugin.checks.EmptyCatchBlockCheck;
import org.sonargosu.plugin.checks.GosuCheck;
import org.sonargosu.plugin.checks.LineLengthCheck;
import org.sonargosu.plugin.checks.ParsingErrorCheck;
import org.sonargosu.plugin.checks.PrintStatementCheck;
import org.sonargosu.plugin.checks.TodoCommentCheck;
import org.sonargosu.plugin.checks.TooManyParametersCheck;

/**
 * Single source of truth for every rule: its metadata and the check that implements it.
 * To add a rule, write a {@link GosuCheck} and add a constant here.
 */
public enum GosuRule {

  LINE_LENGTH("LineLength", "Lines should not be too long",
    "<p>Long lines are hard to read, especially in side-by-side diffs.</p>",
    Severity.MINOR, RuleType.CODE_SMELL, LineLengthCheck::new,
    List.of(new Param(LineLengthCheck.MAX_PARAM, "Maximum authorized line length", String.valueOf(LineLengthCheck.DEFAULT_MAX)))),

  TODO_COMMENT("TodoComment", "Track uses of \"TODO\" and \"FIXME\" tags",
    "<p><code>TODO</code> and <code>FIXME</code> comments mark work that still has to be done. "
      + "This rule makes sure they are not forgotten.</p>",
    Severity.INFO, RuleType.CODE_SMELL, TodoCommentCheck::new, List.of()),

  EMPTY_CATCH("EmptyCatchBlock", "Exceptions should not be silently swallowed",
    "<p>An empty <code>catch</code> block hides failures. Handle or log the exception, "
      + "or add a comment explaining why ignoring it is safe.</p>"
      + "<h2>Noncompliant Code Example</h2><pre>try {\n  doSomething()\n} catch (e : Exception) {\n}</pre>"
      + "<h2>Compliant Solution</h2><pre>try {\n  doSomething()\n} catch (e : Exception) {\n  _logger.error(\"doSomething failed\", e)\n}</pre>",
    Severity.MAJOR, RuleType.BUG, EmptyCatchBlockCheck::new, List.of()),

  PRINT_STATEMENT("PrintStatement", "Loggers should be used instead of \"print\" and System.out",
    "<p>Output written with <code>print()</code> or <code>System.out</code> bypasses log levels, "
      + "formatting and appenders. Use the application logger instead.</p>",
    Severity.MAJOR, RuleType.CODE_SMELL, PrintStatementCheck::new, List.of()),

  TOO_MANY_PARAMETERS("TooManyParameters", "Functions should not have too many parameters",
    "<p>A long parameter list usually means the function does too much, or that "
      + "the parameters belong together in a class or structure.</p>",
    Severity.MAJOR, RuleType.CODE_SMELL, TooManyParametersCheck::new,
    List.of(new Param(TooManyParametersCheck.MAX_PARAM, "Maximum authorized number of parameters", String.valueOf(TooManyParametersCheck.DEFAULT_MAX)))),

  DUPLICATE_USES("DuplicateUses", "\"uses\" statements should not be duplicated",
    "<p>Importing the same type twice is redundant and clutters the file header.</p>",
    Severity.MINOR, RuleType.CODE_SMELL, DuplicateUsesCheck::new, List.of()),

  PARSING_ERROR("ParsingError", "Gosu parser failure",
    "<p>The analyzer could not parse this file, so rules that need the syntax tree were not run on it. "
      + "Either the file contains a syntax error, or it uses syntax the analyzer does not support yet.</p>",
    Severity.MAJOR, RuleType.CODE_SMELL, ParsingErrorCheck::new, List.of());

  public record Param(String key, String description, String defaultValue) {
  }

  private final String key;
  private final String title;
  private final String htmlDescription;
  private final String severity;
  private final RuleType type;
  private final Supplier<GosuCheck> factory;
  private final List<Param> params;

  GosuRule(String key, String title, String htmlDescription, String severity, RuleType type,
    Supplier<GosuCheck> factory, List<Param> params) {
    this.key = key;
    this.title = title;
    this.htmlDescription = htmlDescription;
    this.severity = severity;
    this.type = type;
    this.factory = factory;
    this.params = params;
  }

  public String key() {
    return key;
  }

  public String title() {
    return title;
  }

  public String htmlDescription() {
    return htmlDescription;
  }

  public String severity() {
    return severity;
  }

  public RuleType type() {
    return type;
  }

  public List<Param> params() {
    return params;
  }

  public GosuCheck newCheck() {
    return factory.get();
  }
}
