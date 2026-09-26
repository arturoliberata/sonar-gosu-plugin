package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class TreeCheckTest {

  @Test
  void tree_checks_skip_unparsable_files() {
    assertThat(issueLines(new PrintStatementCheck(), "class A { print(\"x\") ")).isEmpty();
  }
}
