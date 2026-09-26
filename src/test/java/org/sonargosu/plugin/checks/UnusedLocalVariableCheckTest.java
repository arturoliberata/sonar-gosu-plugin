package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class UnusedLocalVariableCheckTest {

  @Test
  void unused_local_variable() {
    String code = """
      function f() {
        var unused = 1
        var used = 2
        print(used)
        var inTemplate = 3
        print("${inTemplate}")
        using (var r = open()) {
          other()
        }
        var inBlock = 4
        list.each(\\ e -> print(inBlock))
        var assigned = 5
        assigned = 6
      }
      """;
    assertThat(programIssueLines(new UnusedLocalVariableCheck(), code)).containsExactly(2);
  }
}
