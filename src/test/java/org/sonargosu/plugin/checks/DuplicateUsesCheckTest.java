package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class DuplicateUsesCheckTest {

  @Test
  void duplicate_uses() {
    String code = """
      uses java.util.List
      uses java.util.Map
      uses java.util.List

      class A {}
      """;
    assertThat(issueLines(new DuplicateUsesCheck(), code)).containsExactly(3);
  }
}
