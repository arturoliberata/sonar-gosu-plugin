package org.sonargosu.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonargosu.plugin.checks.GosuFile;
import org.sonargosu.plugin.rules.GosuRule;

/**
 * Keeps sample-project/src/acme/ClaimRules.gs honest: it parses, and each new rule
 * finds exactly what its comment says.
 */
class SampleProjectTest {

  @Test
  void claim_rules_sample_triggers_each_rule() throws IOException {
    String code = Files.readString(Path.of("sample-project/src/acme/ClaimRules.gs"), StandardCharsets.UTF_8);
    GosuFile file = new GosuFile(code, "ClaimRules.gs");
    assertThat(file.syntaxErrors()).isEmpty();

    List<String> found = new ArrayList<>();
    for (GosuRule rule : GosuRule.values()) {
      var check = rule.newCheck();
      check.configure(java.util.Map.of());
      check.scan(file, (line, message) -> found.add(rule.key() + ":" + line));
    }

    assertThat(found).containsExactlyInAnyOrder(
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
}
