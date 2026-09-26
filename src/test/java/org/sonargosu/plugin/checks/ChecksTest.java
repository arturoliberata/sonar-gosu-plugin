package org.sonargosu.plugin.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChecksTest {

  /** Parses the snippet as a class file. */
  private static List<Integer> issueLines(GosuCheck check, String code) {
    return issueLines(check, code, "Test.gs");
  }

  /** Parses the snippet as a program, so top-level statements are allowed. */
  private static List<Integer> programIssueLines(GosuCheck check, String code) {
    return issueLines(check, code, "test.gsp");
  }

  private static List<Integer> issueLines(GosuCheck check, String code, String filename) {
    List<Integer> lines = new ArrayList<>();
    check.scan(new GosuFile(code, filename), (line, message) -> lines.add(line));
    return lines;
  }

  @Test
  void line_length() {
    LineLengthCheck check = new LineLengthCheck();
    check.configure(Map.of(LineLengthCheck.MAX_PARAM, "10"));
    assertThat(programIssueLines(check, "short\nthis line is too long\nok")).containsExactly(2);
  }

  @Test
  void todo_comment() {
    String code = "// TODO fix\nvar todo = 1\n/* line\n  FIXME later */\n// mastodon";
    assertThat(programIssueLines(new TodoCommentCheck(), code)).containsExactly(1, 4);
  }

  @Test
  void empty_catch() {
    String code = """
      try { a() } catch (e : Exception) { }
      try { a() } catch (e : Exception) { /* ignored on purpose */ }
      try { a() } catch (e : Exception) { log(e) }
      """;
    assertThat(programIssueLines(new EmptyCatchBlockCheck(), code)).containsExactly(1);
  }

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

  @Test
  void too_many_parameters() {
    TooManyParametersCheck check = new TooManyParametersCheck();
    check.configure(Map.of(TooManyParametersCheck.MAX_PARAM, "2"));
    String code = """
      class A {
        function ok(a : int, m : Map<String, Integer>) {}
        function bad(a : int, b : int, c : int) {}
        construct(a : int, b : block(x : int, y : int) : int) {}
        function none() {}
      }
      """;
    assertThat(issueLines(check, code)).containsExactly(3);
  }

  @Test
  void duplicate_uses() {
    String code = """
      uses java.util.List
      uses java.util.Map
      uses java.util.List

      class A {}
      """;
    assertThat(issueLines(new DuplicateUsesCheck(), code)).containsExactly(3);
  }

  @Test
  void parsing_error() {
    assertThat(issueLines(new ParsingErrorCheck(), "class A {\n  function f( {\n}\n")).containsExactly(2);
    assertThat(issueLines(new ParsingErrorCheck(), "class A {}")).isEmpty();
  }

  @Test
  void tree_checks_skip_unparsable_files() {
    assertThat(issueLines(new PrintStatementCheck(), "class A { print(\"x\") ")).isEmpty();
  }

  @Test
  void templates_are_not_parsed() {
    assertThat(issueLines(new ParsingErrorCheck(), "<% var x = 1 %> Hello ${x}", "page.gst")).isEmpty();
  }
}
