package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonargosu.plugin.checks.CheckTestUtils.programIssueLines;

import org.junit.jupiter.api.Test;

class WeekYearInDatePatternCheckTest {

  @Test
  void week_year_in_date_pattern() {
    String code = """
      var f1 = new SimpleDateFormat("YYYY-MM-dd")
      var f2 = new java.text.SimpleDateFormat("yyyy-MM-dd")
      var f3 = DateTimeFormatter.ofPattern("dd/MM/YYYY")
      var f4 = new SimpleDateFormat("YYYY-'W'ww")
      var f5 = new SimpleDateFormat("yyyy 'Year' MM")
      fmt.applyPattern("YY/MM/dd")
      print("Format: YYYY-MM-DD")
      """;
    assertThat(programIssueLines(new WeekYearInDatePatternCheck(), code)).containsExactly(1, 3, 6);
  }
}
