package org.sonargosu.plugin.checks;

import java.util.ArrayList;
import java.util.List;

/** Runs a check on a code snippet and collects what it reports. */
final class CheckTestUtils {

  private CheckTestUtils() {
  }

  /** Parses the snippet as a class file. */
  static List<Integer> issueLines(GosuCheck check, String code) {
    return issueLines(check, code, "Test.gs");
  }

  /** Parses the snippet as a program, so top-level statements are allowed. */
  static List<Integer> programIssueLines(GosuCheck check, String code) {
    return issueLines(check, code, "test.gsp");
  }

  static List<Integer> issueLines(GosuCheck check, String code, String filename) {
    List<Integer> lines = new ArrayList<>();
    check.scan(new GosuFile(code, filename), (line, message) -> lines.add(line));
    return lines;
  }

  /** Issues as "line: message", for tests that check the message text. */
  static List<String> issues(GosuCheck check, String code, String filename) {
    List<String> issues = new ArrayList<>();
    check.scan(new GosuFile(code, filename), (line, message) -> issues.add(line + ": " + message));
    return issues;
  }
}
