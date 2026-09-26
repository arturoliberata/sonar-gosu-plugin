package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class AllBranchesIdenticalCheckTest {

  @Test
  void all_branches_identical() {
    String code = """
      if (a) { x() } else { x() }
      if (a) { x() } else if (b) { x() } else { x() }
      if (a) { x() } else if (b) { x() }
      var v = a ? 1 : 1
      var w = a ? 1 : 2
      switch (k) { case 1: x() break default: x() }
      switch (k) { case 1: x() break case 2: x() }
      """;
    assertThat(programIssueLines(new AllBranchesIdenticalCheck(), code)).containsExactly(1, 2, 4, 6);
  }
}
