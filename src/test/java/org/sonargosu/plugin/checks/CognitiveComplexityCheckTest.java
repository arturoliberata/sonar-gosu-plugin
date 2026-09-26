package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issues;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CognitiveComplexityCheckTest {

  @Test
  void cognitive_complexity() {
    CognitiveComplexityCheck check = new CognitiveComplexityCheck();
    check.configure(Map.of(CognitiveComplexityCheck.THRESHOLD_PARAM, "3"));
    String code = """
      class A {
        function simple(a : boolean) {
          if (a) { x() }
        }
        function nested(list : List<String>) {
          for (s in list) {
            if (s != null) {
              if (s.length() > 0 and s != "x") {
                x()
              }
            }
          }
        }
        function chain(a : int) {
          if (a == 1) { x() }
          else if (a == 2) { y() }
          else if (a == 3) { z() }
          else { w() }
        }
        function lambdas(list : List<String>) {
          list.each(\\ s -> { if (s == null) { x() } })
        }
      }
      """;
    // nested: for +1, if +2, if +3, "and" +1 = 7.  chain: if, else if, else if, else = 4.  lambdas: 2.
    assertThat(issues(check, code, "A.gs")).containsExactly(
      "5: Refactor this function to reduce its Cognitive Complexity from 7 to the 3 allowed.",
      "14: Refactor this function to reduce its Cognitive Complexity from 4 to the 3 allowed.");
  }
}
