package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class HardcodedCredentialCheckTest {

  @Test
  void hardcoded_credential() {
    String code = """
      var dbPassword = "Summer2024!"
      var passwordLabel = "Enter password"
      var PASSWORD_KEY = "db.password"
      var password = ""
      var pwd = "********"
      var url = "jdbc:oracle:thin:@db;user=gw;password=Summer2024!"
      var url2 = "jdbc:x;password=?"
      var site = "https://admin:s3cret@example.com/api"
      conn.Password = "hunter2"
      connect(:password = "hunter2")
      var props = { "password" -> "hunter2", "user" -> "gw" }
      """;
    assertThat(programIssueLines(new HardcodedCredentialCheck(), code)).containsExactly(1, 6, 8, 9, 10, 11);
  }
}
