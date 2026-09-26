package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class UnusedPrivateFieldCheckTest {

  @Test
  void unused_private_field() {
    String code = """
      class A {
        var _unused : int
        var _used : int
        var _viaTemplate : String
        var _exposed : String as Exposed
        public var Visible : int
        @Inject var _injected : Object
        static var _count = 0
        function f() : String {
          _used = 1
          return "${_viaTemplate}"
        }
      }
      """;
    assertThat(issueLines(new UnusedPrivateFieldCheck(), code)).containsExactly(2, 8);
  }
}
