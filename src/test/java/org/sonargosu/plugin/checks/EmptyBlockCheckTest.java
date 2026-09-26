package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class EmptyBlockCheckTest {

  @Test
  void empty_block() {
    String code = """
      if (a) {
      }
      if (b) {
        // intentionally empty
      }
      while (c) {}
      try { x() } catch (e : Exception) { }
      try { } finally { y() }
      list.each(\\ e -> {})
      function f() {}
      """;
    assertThat(programIssueLines(new EmptyBlockCheck(), code)).containsExactly(1, 6, 8);
  }
}
