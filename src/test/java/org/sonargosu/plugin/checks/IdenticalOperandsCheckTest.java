package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class IdenticalOperandsCheckTest {

  @Test
  void identical_operands() {
    String code = """
      var r1 = a == a
      var r2 = a == b
      var r3 = x - x
      var r4 = x + x
      var r5 = x * x
      var r6 = f() == f()
      var r7 = x - y - y
      var r8 = ready and ready
      var r9 = p.StartDate == p.StartDate
      var r10 = a <= a
      var r11 = 5 kg
      var r12 = 5 == 5
      var r13 = true != true
      var r14 = "foo" == "foo"
      """;
    assertThat(programIssueLines(new IdenticalOperandsCheck(), code)).containsExactly(1, 3, 8, 9, 10);
  }
}
