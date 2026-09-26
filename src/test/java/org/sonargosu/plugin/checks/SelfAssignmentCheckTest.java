package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class SelfAssignmentCheckTest {

  @Test
  void self_assignment() {
    String code = """
      var x = 1
      x = x
      x = y
      a.b = a.b
      a.get().b = a.get().b
      x += x
      """;
    assertThat(programIssueLines(new SelfAssignmentCheck(), code)).containsExactly(2, 4);
  }
}
