package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class TodoCommentCheckTest {

  @Test
  void todo_comment() {
    String code = "// TODO fix\nvar todo = 1\n/* line\n  FIXME later */\n// mastodon";
    assertThat(programIssueLines(new TodoCommentCheck(), code)).containsExactly(1, 4);
  }
}
