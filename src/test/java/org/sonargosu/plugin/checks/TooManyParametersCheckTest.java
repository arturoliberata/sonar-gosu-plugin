package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TooManyParametersCheckTest {

  @Test
  void too_many_parameters() {
    TooManyParametersCheck check = new TooManyParametersCheck();
    check.configure(Map.of(TooManyParametersCheck.MAX_PARAM, "2"));
    String code = """
      class A {
        function ok(a : int, m : Map<String, Integer>) {}
        function bad(a : int, b : int, c : int) {}
        construct(a : int, b : block(x : int, y : int) : int) {}
        function none() {}
      }
      """;
    assertThat(issueLines(check, code)).containsExactly(3);
  }
}
