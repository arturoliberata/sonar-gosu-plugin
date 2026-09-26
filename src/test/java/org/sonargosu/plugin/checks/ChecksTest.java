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
}
