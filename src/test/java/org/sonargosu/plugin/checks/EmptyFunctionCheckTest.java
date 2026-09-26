package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class EmptyFunctionCheckTest {

  @Test
  void empty_function() {
    String code = """
      abstract class A {
        function empty() {}
        function commented() {
          // nothing to do
        }
        construct() {}
        property get Name() : String { return "" }
        property set Name(v : String) {}
        abstract function noBody()
      }
      """;
    assertThat(issueLines(new EmptyFunctionCheck(), code)).containsExactly(2, 8);
  }
}
