package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class UnusedPrivateFunctionCheckTest {

  @Test
  void unused_private_function() {
    String code = """
      class A {
        private function unused() {}
        private function used() {}
        private function viaFeatureLiteral() {}
        function pub() {
          used()
          var f = A#viaFeatureLiteral()
        }
        @SomeAnnotation private function annotated() {}
        function notPrivate() {}
      }
      """;
    assertThat(issueLines(new UnusedPrivateFunctionCheck(), code)).containsExactly(2);
  }
}
