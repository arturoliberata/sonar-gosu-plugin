package org.sonargosu.plugin.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonargosu.plugin.checks.CollapsibleIfCheck;
import org.sonargosu.plugin.checks.DuplicateUsesCheck;
import org.sonargosu.plugin.checks.EmptyBlockCheck;
import org.sonargosu.plugin.checks.EmptyCatchBlockCheck;
import org.sonargosu.plugin.checks.EmptyFunctionCheck;
import org.sonargosu.plugin.checks.GosuCheck;
import org.sonargosu.plugin.checks.IdenticalOperandsCheck;
import org.sonargosu.plugin.checks.JumpInFinallyCheck;
import org.sonargosu.plugin.checks.LineLengthCheck;
import org.sonargosu.plugin.checks.ParsingErrorCheck;
import org.sonargosu.plugin.checks.PrintStatementCheck;
import org.sonargosu.plugin.checks.SelfAssignmentCheck;
import org.sonargosu.plugin.checks.SwitchWithoutDefaultCheck;
import org.sonargosu.plugin.checks.TodoCommentCheck;
import org.sonargosu.plugin.checks.TooManyParametersCheck;
import org.sonargosu.plugin.checks.UnusedLocalVariableCheck;
import org.sonargosu.plugin.checks.UnusedPrivateFieldCheck;
import org.sonargosu.plugin.checks.UnusedPrivateFunctionCheck;

/**
 * Single source of truth for every rule: its metadata and the check that implements it.
 * To add a rule, write a {@link GosuCheck}, add a constant here, and add its description as
 * {@code src/main/resources/org/sonargosu/plugin/rules/<Key>.html}.
 */
public enum GosuRule {

  // --- Bugs ---
  EMPTY_CATCH("EmptyCatchBlock", "Exceptions should not be silently swallowed",
    Severity.MAJOR, RuleType.BUG, EmptyCatchBlockCheck::new),
  JUMP_IN_FINALLY("JumpInFinally", "Jump statements should not be used in \"finally\" blocks",
    Severity.CRITICAL, RuleType.BUG, JumpInFinallyCheck::new),
  SELF_ASSIGNMENT("SelfAssignment", "Variables should not be assigned to themselves",
    Severity.MAJOR, RuleType.BUG, SelfAssignmentCheck::new),
  IDENTICAL_OPERANDS("IdenticalOperands", "Identical expressions should not be used on both sides of an operator",
    Severity.MAJOR, RuleType.BUG, IdenticalOperandsCheck::new),

  // --- Code smells ---
  EMPTY_BLOCK("EmptyBlock", "Blocks of code should not be left empty",
    Severity.MAJOR, RuleType.CODE_SMELL, EmptyBlockCheck::new),
  EMPTY_FUNCTION("EmptyFunction", "Functions should not be empty",
    Severity.MAJOR, RuleType.CODE_SMELL, EmptyFunctionCheck::new),
  SWITCH_WITHOUT_DEFAULT("SwitchWithoutDefault", "\"switch\" statements should have a \"default\" case",
    Severity.MAJOR, RuleType.CODE_SMELL, SwitchWithoutDefaultCheck::new),
  COLLAPSIBLE_IF("CollapsibleIf", "Nested \"if\" statements that can be merged should be merged",
    Severity.MINOR, RuleType.CODE_SMELL, CollapsibleIfCheck::new),
  UNUSED_LOCAL_VARIABLE("UnusedLocalVariable", "Unused local variables should be removed",
    Severity.MINOR, RuleType.CODE_SMELL, UnusedLocalVariableCheck::new),
  UNUSED_PRIVATE_FIELD("UnusedPrivateField", "Unused private fields should be removed",
    Severity.MAJOR, RuleType.CODE_SMELL, UnusedPrivateFieldCheck::new),
  UNUSED_PRIVATE_FUNCTION("UnusedPrivateFunction", "Unused private functions should be removed",
    Severity.MAJOR, RuleType.CODE_SMELL, UnusedPrivateFunctionCheck::new),
  PRINT_STATEMENT("PrintStatement", "Loggers should be used instead of \"print\" and System.out",
    Severity.MAJOR, RuleType.CODE_SMELL, PrintStatementCheck::new),
  TOO_MANY_PARAMETERS("TooManyParameters", "Functions should not have too many parameters",
    Severity.MAJOR, RuleType.CODE_SMELL, TooManyParametersCheck::new,
    new Param(TooManyParametersCheck.MAX_PARAM, "Maximum authorized number of parameters", String.valueOf(TooManyParametersCheck.DEFAULT_MAX))),
  DUPLICATE_USES("DuplicateUses", "\"uses\" statements should not be duplicated",
    Severity.MINOR, RuleType.CODE_SMELL, DuplicateUsesCheck::new),
  LINE_LENGTH("LineLength", "Lines should not be too long",
    Severity.MINOR, RuleType.CODE_SMELL, LineLengthCheck::new,
    new Param(LineLengthCheck.MAX_PARAM, "Maximum authorized line length", String.valueOf(LineLengthCheck.DEFAULT_MAX))),
  TODO_COMMENT("TodoComment", "Track uses of \"TODO\" and \"FIXME\" tags",
    Severity.INFO, RuleType.CODE_SMELL, TodoCommentCheck::new),
  PARSING_ERROR("ParsingError", "Gosu parser failure",
    Severity.MAJOR, RuleType.CODE_SMELL, ParsingErrorCheck::new);

  public record Param(String key, String description, String defaultValue) {
  }

  private final String key;
  private final String title;
  private final String severity;
  private final RuleType type;
  private final Supplier<GosuCheck> factory;
  private final List<Param> params;

  GosuRule(String key, String title, String severity, RuleType type, Supplier<GosuCheck> factory, Param... params) {
    this.key = key;
    this.title = title;
    this.severity = severity;
    this.type = type;
    this.factory = factory;
    this.params = List.of(params);
  }

  public String key() {
    return key;
  }

  public String title() {
    return title;
  }

  /** Loaded from {@code <Key>.html} next to this class. */
  public String htmlDescription() {
    String resource = key + ".html";
    try (InputStream in = GosuRule.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Missing rule description " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
