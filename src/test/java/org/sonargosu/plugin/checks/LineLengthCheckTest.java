package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LineLengthCheckTest {

  @Test
  void line_length() {
    LineLengthCheck check = new LineLengthCheck();
    check.configure(Map.of(LineLengthCheck.MAX_PARAM, "10"));
    assertThat(programIssueLines(check, "short\nthis line is too long\nok")).containsExactly(2);
  }
}
