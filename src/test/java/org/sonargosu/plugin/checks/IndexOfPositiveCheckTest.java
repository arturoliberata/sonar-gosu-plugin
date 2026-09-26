package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class IndexOfPositiveCheckTest {

  @Test
  void index_of_positive() {
    String code = """
      if (s.indexOf("a") > 0) { x() }
      if (s.indexOf("a") >= 0) { x() }
      if (0 < s.lastIndexOf("a")) { x() }
      if (s.indexOf("a") > 1) { x() }
      if (count > 0) { x() }
      """;
    assertThat(programIssueLines(new IndexOfPositiveCheck(), code)).containsExactly(1, 3);
  }
}
