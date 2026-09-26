package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class BigDecimalFromDoubleCheckTest {

  @Test
  void big_decimal_from_double() {
    String code = """
      var a = new BigDecimal(0.1)
      var b = new java.math.BigDecimal(2.5d)
      var c = new BigDecimal("0.1")
      var d = new BigDecimal(1)
      var e = new BigDecimal(0.1bd)
      var f = new BigDecimal(-1e3)
      var g = BigDecimal.valueOf(0.1)
      """;
    assertThat(programIssueLines(new BigDecimalFromDoubleCheck(), code)).containsExactly(1, 2, 6);
  }
}
