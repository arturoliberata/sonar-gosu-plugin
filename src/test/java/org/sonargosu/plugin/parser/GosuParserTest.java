package org.sonargosu.plugin.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonargosu.plugin.parser.GosuParserFacade.Kind;

class GosuParserTest {

  private static void assertParses(String code, Kind kind) {
    GosuParseResult result = GosuParserFacade.parse(code, kind);
    assertThat(result.errors()).as("syntax errors").isEmpty();
  }

  @Test
  void parses_class_with_common_constructs() {
    assertParses("""
      package acme.claims

      uses java.util.List
      uses java.util.Map
      uses gw.api.util.*

      @Deprecated
      class ClaimHelper<T extends Number> extends Base implements Runnable, Comparable<ClaimHelper> {

        static final var MAX : int = 10
        var _names : List<String> as readonly Names = {}
        delegate _r represents Runnable = new Thread()

        construct(amount : int, label : String = "x") {
          super()
          _names.add(label)
        }

        property get Count() : int {
          return _names.Count
        }

        override function run() {
          for (name in _names index i) {
            if (name.length() > 3 and i < MAX) {
              print("${name} at ${i}")
            } else {
              continue
            }
          }
          var total = 0
          while (total < 10) { total += 2 }
          do { total-- } while (total > 0)
          switch (total) {
            case 0: return
            default: break
          }
        }

        function convert(x : Object) : String {
          if (x typeis String) {
            return x as String
          }
          var m = new java.util.HashMap<String, Integer>() { "a" -> 1, "b" -> 2 }
          var squares = _names.map(\\ s -> s.length() * s.length())
          var nums = new int[] {1, 2, 3}
          var r = 1..10
          var shifted = total() >> 2
          try {
            return m.get("a")?.toString() ?: "none"
          } catch (e : Exception) {
            throw new RuntimeException(e)
          } finally {
            _names.clear()
          }
        }

        private function total() : int { return 0 }
      }
      """, Kind.CLASS);
  }

  @Test
  void parses_enhancement() {
    assertParses("""
      package acme

      enhancement StringEnhancement : String {
        function shout() : String {
          return this.toUpperCase() + "!"
        }
      }
      """, Kind.CLASS);
  }

  @Test
  void parses_enum_and_interface() {
    assertParses("""
      enum Color { RED, GREEN, BLUE
        function isWarm() : boolean { return this == RED }
      }
      """, Kind.CLASS);
    assertParses("""
      interface Shape {
        function area() : double
        property get Name() : String
      }
      """, Kind.CLASS);
  }

  @Test
  void parses_program() {
    assertParses("""
      uses java.util.ArrayList

      var list = new ArrayList<String>()
      list.add("a")
      function greet(name : String) {
        print("Hello " + name)
      }
      greet("world")
      """, Kind.PROGRAM);
  }

  /** Syntax newer than the 2013 grammar, found by parsing the gosu-lang sources. */
  @Test
  void parses_modern_gosu_features() {
    assertParses("""
      uses gw.util.science.UnitConstants#min
      uses gw.util.science.MetricScaleUnit#*
      uses gw.test.TestClass#assertEquals(Object, Object)

      class Modern {
        property Name : String
        property get Prop3 : String = "aaa"
        var mass = 5 kg
        var momentum = kg m/s
        var pi = 3.14159r
        var f : java.util.function.Function = \\ o : Object -> 8

        @get:MyMethodAnno(1)
        var x : int

        reified function mapRight<R1>(f : (arg : String) : R1) : R1 {
          return f("a")
        }

        function strings() : String {
          var a = "A \\
      B"
          foo(:override = true)
          var c = '$'
          var half = 1. / 2
          return "${ "${42}" }"
        }

        function listeners() {
          addListener(\\ board ->
            {
              board.a()
              board.b()
            })
        }
      }
      """, Kind.CLASS);
    assertParses("""
      annotation BinderSeparators {
        function accepted() : String[] = {}
      }
      """, Kind.CLASS);
    assertParses("""
      interface ITimeOfDay {
        property get Hour() : Integer
        property get Min() : Integer { return 0 }
        static function of(h : int) : ITimeOfDay { return null }
      }
      """, Kind.CLASS);
    assertParses("""
      extends MyProgramBase

      print("hi")
      """, Kind.PROGRAM);
  }

  @Test
  void call_arguments_must_start_on_the_same_line() {
    GosuParseResult result = GosuParserFacade.parse("""
      class A {
        function f() {
          foo()
          (x as String).length()
        }
      }
      """, Kind.CLASS);
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void reports_syntax_errors_with_position() {
    GosuParseResult result = GosuParserFacade.parse("class A {\n  function f( {\n}\n", Kind.CLASS);
    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).line()).isEqualTo(2);
  }

  @Test
  void picks_kind_from_extension() {
    assertThat(GosuParserFacade.kindOf("Run.gsp")).isEqualTo(Kind.PROGRAM);
    assertThat(GosuParserFacade.kindOf("A.gs")).isEqualTo(Kind.CLASS);
    assertThat(GosuParserFacade.kindOf("B.gsx")).isEqualTo(Kind.CLASS);
  }
}
