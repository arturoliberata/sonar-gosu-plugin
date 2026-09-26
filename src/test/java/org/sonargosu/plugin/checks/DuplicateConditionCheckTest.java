package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class DuplicateConditionCheckTest {

  @Test
  void duplicate_condition() {
    String code = """
      if (a) { x() } else if (b) { y() } else if (a) { z() }
      if (a) { x() }
      else if (b) { y() }
      else if (b) { z() }
      if (a) { x() } else { if (a) { y() } }
      """;
    assertThat(programIssueLines(new DuplicateConditionCheck(), code)).containsExactly(1, 4);
  }
}
