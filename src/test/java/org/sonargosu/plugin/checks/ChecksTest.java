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

  @Test
  void empty_block() {
    String code = """
      if (a) {
      }
      if (b) {
        // intentionally empty
      }
      while (c) {}
      try { x() } catch (e : Exception) { }
      try { } finally { y() }
      list.each(\\ e -> {})
      function f() {}
      """;
    assertThat(programIssueLines(new EmptyBlockCheck(), code)).containsExactly(1, 6, 8);
  }

  @Test
  void switch_without_default() {
    String code = """
      switch (x) { case 1: print("one") }
      switch (x) { case 1: print("one") default: print("other") }
      """;
    assertThat(programIssueLines(new SwitchWithoutDefaultCheck(), code)).containsExactly(1);
  }

  @Test
  void collapsible_if() {
    String code = """
      if (a) {
        if (b) {
          x()
        }
      }
      if (a) if (b) x()
      if (a) {
        if (b) { x() } else { y() }
      }
      if (a) {
        if (b) { x() }
      } else { z() }
      if (a) {
        if (b) { x() }
        y()
      }
      """;
    assertThat(programIssueLines(new CollapsibleIfCheck(), code)).containsExactly(2, 6);
  }

  @Test
  void unused_private_field() {
    String code = """
      class A {
        var _unused : int
        var _used : int
        var _viaTemplate : String
        var _exposed : String as Exposed
        public var Visible : int
        @Inject var _injected : Object
        static var _count = 0
        function f() : String {
          _used = 1
          return "${_viaTemplate}"
        }
      }
      """;
    assertThat(issueLines(new UnusedPrivateFieldCheck(), code)).containsExactly(2, 8);
  }

  @Test
  void jump_in_finally() {
    String code = """
      function f() : int {
        try {
          return 1
        } finally {
          return 2
        }
      }
      function g() {
        try { a() } finally {
          for (x in list) { if (x == null) break }
          throw new RuntimeException()
        }
      }
      function h() {
        for (x in list) {
          try { a() } finally { continue }
        }
        try { a() } finally { list.each(\\ e -> { return }) }
      }
      """;
    assertThat(programIssueLines(new JumpInFinallyCheck(), code)).containsExactly(5, 11, 16);
  }

  @Test
  void unused_private_function() {
    String code = """
      class A {
        private function unused() {}
        private function used() {}
        private function viaFeatureLiteral() {}
        function pub() {
          used()
          var f = A#viaFeatureLiteral()
        }
        @SomeAnnotation private function annotated() {}
        function notPrivate() {}
      }
      """;
    assertThat(issueLines(new UnusedPrivateFunctionCheck(), code)).containsExactly(2);
  }

  @Test
  void empty_function() {
    String code = """
      abstract class A {
        function empty() {}
        function commented() {
          // nothing to do
        }
        construct() {}
        property get Name() : String { return "" }
        property set Name(v : String) {}
        abstract function noBody()
      }
      """;
    assertThat(issueLines(new EmptyFunctionCheck(), code)).containsExactly(2, 8);
  }

  @Test
  void unused_local_variable() {
    String code = """
      function f() {
        var unused = 1
        var used = 2
        print(used)
        var inTemplate = 3
        print("${inTemplate}")
        using (var r = open()) {
          other()
        }
        var inBlock = 4
        list.each(\\ e -> print(inBlock))
        var assigned = 5
        assigned = 6
      }
      """;
    assertThat(programIssueLines(new UnusedLocalVariableCheck(), code)).containsExactly(2);
  }

  @Test
  void self_assignment() {
    String code = """
      var x = 1
      x = x
      x = y
      a.b = a.b
      a.get().b = a.get().b
      x += x
      """;
    assertThat(programIssueLines(new SelfAssignmentCheck(), code)).containsExactly(2, 4);
  }

  @Test
  void identical_operands() {
    String code = """
      var r1 = a == a
      var r2 = a == b
      var r3 = x - x
      var r4 = x + x
      var r5 = x * x
      var r6 = f() == f()
      var r7 = x - y - y
      var r8 = ready and ready
      var r9 = p.StartDate == p.StartDate
      var r10 = a <= a
      var r11 = 5 kg
      var r12 = 5 == 5
      var r13 = true != true
      var r14 = "foo" == "foo"
      """;
    assertThat(programIssueLines(new IdenticalOperandsCheck(), code)).containsExactly(1, 3, 8, 9, 10);
  }

  @Test
  void duplicate_condition() {
    String code = """
      if (a) { x() } else if (b) { y() } else if (a) { z() }
      if (a) { x() }
      else if (b) { y() }
      else if (b) { z() }
      if (a) { x() } else { if (a) { y() } }
      """;
    assertThat(programIssueLines(new DuplicateConditionCheck(), code)).containsExactly(1, 4);
  }

  @Test
  void duplicate_branch() {
    String code = """
      if (a) {
        x()
        y()
      } else if (b) {
        x()
        y()
      } else {
        z()
      }
      if (a) { x() } else if (b) { x() } else { z() }
      if (a) {
        x()
        y()
      } else {
        x()
        y()
      }
      switch (k) {
        case 1:
          x()
          y()
          break
        case 2:
          x()
          y()
          break
        default:
          z()
      }
      """;
    assertThat(programIssueLines(new DuplicateBranchCheck(), code)).containsExactly(4, 23);
  }

  @Test
  void all_branches_identical() {
    String code = """
      if (a) { x() } else { x() }
      if (a) { x() } else if (b) { x() } else { x() }
      if (a) { x() } else if (b) { x() }
      var v = a ? 1 : 1
      var w = a ? 1 : 2
      switch (k) { case 1: x() break default: x() }
      switch (k) { case 1: x() break case 2: x() }
      """;
    assertThat(programIssueLines(new AllBranchesIdenticalCheck(), code)).containsExactly(1, 2, 4, 6);
  }

  @Test
  void cognitive_complexity() {
    CognitiveComplexityCheck check = new CognitiveComplexityCheck();
    check.configure(Map.of(CognitiveComplexityCheck.THRESHOLD_PARAM, "3"));
    String code = """
      class A {
        function simple(a : boolean) {
          if (a) { x() }
        }
        function nested(list : List<String>) {
          for (s in list) {
            if (s != null) {
              if (s.length() > 0 and s != "x") {
                x()
              }
            }
          }
        }
        function chain(a : int) {
          if (a == 1) { x() }
          else if (a == 2) { y() }
          else if (a == 3) { z() }
          else { w() }
        }
        function lambdas(list : List<String>) {
          list.each(\\ s -> { if (s == null) { x() } })
        }
      }
      """;
    List<String> messages = new ArrayList<>();
    check.scan(new GosuFile(code, "A.gs"), (line, message) -> messages.add(line + ": " + message));
    // nested: for +1, if +2, if +3, "and" +1 = 7.  chain: if, else if, else if, else = 4.  lambdas: 2.
    assertThat(messages).containsExactly(
      "5: Refactor this function to reduce its Cognitive Complexity from 7 to the 3 allowed.",
      "14: Refactor this function to reduce its Cognitive Complexity from 4 to the 3 allowed.");
  }

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

  @Test
  void big_decimal_from_double() {
    String code = """
      var a = new BigDecimal(0.1)
      var b = new java.math.BigDecimal(2.5d)
      var c = new BigDecimal("0.1")
      var d = new BigDecimal(1)
      var e = new BigDecimal(0.1bd)
      var f = new BigDecimal(-1e3)
      var g = BigDecimal.valueOf(0.1)
      """;
    assertThat(programIssueLines(new BigDecimalFromDoubleCheck(), code)).containsExactly(1, 2, 6);
  }

  @Test
  void exception_not_thrown() {
    String code = """
      new IllegalStateException("x")
      throw new IllegalStateException("x")
      var e = new RuntimeException()
      new Foo()
      new java.lang.Error("boom")
      new StringBuilder().append("x")
      """;
    assertThat(programIssueLines(new ExceptionNotThrownCheck(), code)).containsExactly(1, 5);
  }

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

  @Test
  void index_of_positive() {
    String code = """
      if (s.indexOf("a") > 0) { x() }
      if (s.indexOf("a") >= 0) { x() }
      if (0 < s.lastIndexOf("a")) { x() }
      if (s.indexOf("a") > 1) { x() }
      if (count > 0) { x() }
      """;
    assertThat(programIssueLines(new IndexOfPositiveCheck(), code)).containsExactly(1, 3);
  }
}
