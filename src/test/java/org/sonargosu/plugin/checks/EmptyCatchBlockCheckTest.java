package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class EmptyCatchBlockCheckTest {

  @Test
  void empty_catch() {
    String code = """
      try { a() } catch (e : Exception) { }
      try { a() } catch (e : Exception) { /* ignored on purpose */ }
      try { a() } catch (e : Exception) { log(e) }
      """;
    assertThat(programIssueLines(new EmptyCatchBlockCheck(), code)).containsExactly(1);
  }
}
