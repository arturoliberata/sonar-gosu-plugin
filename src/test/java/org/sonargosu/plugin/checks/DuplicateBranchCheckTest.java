package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class DuplicateBranchCheckTest {

  @Test
  void duplicate_branch() {
    String code = """
      if (a) {
        x()
        y()
      } else if (b) {
        x()
        y()
      } else {
        z()
      }
      if (a) { x() } else if (b) { x() } else { z() }
      if (a) {
        x()
        y()
      } else {
        x()
        y()
      }
      switch (k) {
        case 1:
          x()
          y()
          break
        case 2:
          x()
          y()
          break
        default:
          z()
      }
      """;
    assertThat(programIssueLines(new DuplicateBranchCheck(), code)).containsExactly(4, 23);
  }
}
