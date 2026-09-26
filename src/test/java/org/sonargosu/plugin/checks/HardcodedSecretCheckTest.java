package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class HardcodedSecretCheckTest {

  @Test
  void hardcoded_secret() {
    // Fake keys are assembled at runtime so that secret scanners (such as GitHub push
    // protection) don't mistake this test file for a leaked credential.
    String awsKey = "AKIA" + "IOSFODNN7EXAMPLE";
    String githubToken = "ghp" + "_1234567890abcdefghijABCDEFGHIJ123456";
    String code = """
      var ratingApiKey = "k3J9x7Qm2pL8vT4nR6wZ"
      var tokenHeader = "Authorization"
      var accessToken = "abc"
      var authToken = "auth.token.header.name"
      var awsKey = "%s"
      var gh = "%s"
      var clientSecret = "aaaaaaaaaaaaaaaa"
      """.formatted(awsKey, githubToken);
    assertThat(programIssueLines(new HardcodedSecretCheck(), code)).containsExactly(1, 5, 6);
  }
}
