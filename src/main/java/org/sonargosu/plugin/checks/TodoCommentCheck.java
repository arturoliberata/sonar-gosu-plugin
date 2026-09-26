package org.sonargosu.plugin.checks;

import java.util.regex.Pattern;
import org.sonargosu.plugin.lexer.Token;

public class TodoCommentCheck implements GosuCheck {

  private static final Pattern TAG = Pattern.compile("(?i)\\b(TODO|FIXME)\\b");

  @Override
  public void scan(GosuFile file, IssueCollector issues) {
    for (Token token : file.tokens()) {
      if (!token.type().isComment()) {
        continue;
      }
      String[] commentLines = token.text().split("\r\n|\r|\n", -1);
      for (int i = 0; i < commentLines.length; i++) {
        if (TAG.matcher(commentLines[i]).find()) {
          issues.report(token.line() + i, "Complete the task associated with this TODO/FIXME comment.");
        }
      }
    }
  }
}
