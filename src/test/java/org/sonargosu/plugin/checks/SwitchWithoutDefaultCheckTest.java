package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class SwitchWithoutDefaultCheckTest {

  @Test
  void switch_without_default() {
    String code = """
      switch (x) { case 1: print("one") }
      switch (x) { case 1: print("one") default: print("other") }
      """;
    assertThat(programIssueLines(new SwitchWithoutDefaultCheck(), code)).containsExactly(1);
  }
}
