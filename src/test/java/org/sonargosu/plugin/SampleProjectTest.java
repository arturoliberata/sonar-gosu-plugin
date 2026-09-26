package org.sonargosu.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonargosu.plugin.checks.GosuCheck;
import org.sonargosu.plugin.checks.GosuFile;
import org.sonargosu.plugin.rules.GosuRule;

/**
 * Keeps the sample files in sample-project/src/acme honest: they parse, and every rule
 * finds exactly what their comments say, nothing more.
 */
class SampleProjectTest {

  private static List<String> issues(String fileName) throws IOException {
    String code = Files.readString(Path.of("sample-project/src/acme", fileName), StandardCharsets.UTF_8);
    GosuFile file = new GosuFile(code, fileName);
    assertThat(file.syntaxErrors()).isEmpty();

    List<String> found = new ArrayList<>();
    for (GosuRule rule : GosuRule.values()) {
      GosuCheck check = rule.newCheck();
      check.configure(Map.of());
      check.scan(file, (line, message) -> found.add(rule.key() + ":" + line));
    }
    return found;
  }

  @Test
  void claim_rules_sample() throws IOException {
    assertThat(issues("ClaimRules.gs")).containsExactlyInAnyOrder(
      "UnusedPrivateField:11",
      "UnusedPrivateFunction:18",
      "EmptyFunction:23",
      "CollapsibleIf:29",
      "EmptyBlock:33",
      "UnusedLocalVariable:39",
      "SwitchWithoutDefault:40",
      "JumpInFinally:54",
      "SelfAssignment:60",
      "IdenticalOperands:61");
  }

  @Test
  void rating_rules_sample() throws IOException {
    assertThat(issues("RatingRules.gs")).containsExactlyInAnyOrder(
      "HardcodedCredential:12",
      "HardcodedSecret:15",
      "BigDecimalFromDouble:23",
      "WeekYearInDatePattern:28",
      "ExceptionNotThrown:34",
      "IndexOfPositive:40",
      "DuplicateCondition:49",
      "DuplicateBranch:60",
      "AllBranchesIdentical:70",
      "CognitiveComplexity:74");
  }
}
