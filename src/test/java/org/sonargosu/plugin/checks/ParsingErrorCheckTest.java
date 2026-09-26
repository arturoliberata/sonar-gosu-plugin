package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.issueLines;

import org.junit.jupiter.api.Test;

class ParsingErrorCheckTest {

  @Test
  void parsing_error() {
    assertThat(issueLines(new ParsingErrorCheck(), "class A {\n  function f( {\n}\n")).containsExactly(2);
    assertThat(issueLines(new ParsingErrorCheck(), "class A {}")).isEmpty();
  }

  @Test
  void templates_are_not_parsed() {
    assertThat(issueLines(new ParsingErrorCheck(), "<% var x = 1 %> Hello ${x}", "page.gst")).isEmpty();
  }
}
