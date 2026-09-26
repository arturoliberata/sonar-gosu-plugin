package org.sonargosu.plugin.checks;

import java.util.Map;

public class LineLengthCheck implements GosuCheck {

  public static final String MAX_PARAM = "maximumLineLength";
  public static final int DEFAULT_MAX = 120;

  private int max = DEFAULT_MAX;

  @Override
  public void configure(Map<String, String> params) {
    max = Integer.parseInt(params.getOrDefault(MAX_PARAM, String.valueOf(DEFAULT_MAX)).trim());
  }

  @Override
  public void scan(GosuFile file, IssueCollector issues) {
    String[] lines = file.lines();
    for (int i = 0; i < lines.length; i++) {
      int length = lines[i].length();
      if (length > max) {
        issues.report(i + 1, "Split this " + length + " characters long line (which is greater than " + max + " authorized).");
      }
    }
  }
}
