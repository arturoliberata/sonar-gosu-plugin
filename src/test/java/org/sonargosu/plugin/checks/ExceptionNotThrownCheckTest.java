package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class ExceptionNotThrownCheckTest {

  @Test
  void exception_not_thrown() {
    String code = """
      new IllegalStateException("x")
      throw new IllegalStateException("x")
      var e = new RuntimeException()
      new Foo()
      new java.lang.Error("boom")
      new StringBuilder().append("x")
      """;
    assertThat(programIssueLines(new ExceptionNotThrownCheck(), code)).containsExactly(1, 5);
  }
}
