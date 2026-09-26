package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class CollapsibleIfCheckTest {

  @Test
  void collapsible_if() {
    String code = """
      if (a) {
        if (b) {
          x()
        }
      }
      if (a) if (b) x()
      if (a) {
        if (b) { x() } else { y() }
      }
      if (a) {
        if (b) { x() }
      } else { z() }
      if (a) {
        if (b) { x() }
        y()
      }
      """;
    assertThat(programIssueLines(new CollapsibleIfCheck(), code)).containsExactly(2, 6);
  }
}
