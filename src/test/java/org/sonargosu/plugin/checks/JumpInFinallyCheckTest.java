package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class JumpInFinallyCheckTest {

  @Test
  void jump_in_finally() {
    String code = """
      function f() : int {
        try {
          return 1
        } finally {
          return 2
        }
      }
      function g() {
        try { a() } finally {
          for (x in list) { if (x == null) break }
          throw new RuntimeException()
        }
      }
      function h() {
        for (x in list) {
          try { a() } finally { continue }
        }
        try { a() } finally { list.each(\\ e -> { return }) }
      }
      """;
    assertThat(programIssueLines(new JumpInFinallyCheck(), code)).containsExactly(5, 11, 16);
  }
}
