package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class PrintStatementCheckTest {

  @Test
  void print_statement() {
    String code = """
      print("hello")
      logger.print("fine")
      System.out.println("x")
      function print(s : String) {}
      var s = "print(x)"
      """;
    assertThat(programIssueLines(new PrintStatementCheck(), code)).containsExactly(1, 3);
  }
}
