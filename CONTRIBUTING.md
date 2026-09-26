# Contributing

Thanks for helping improve Gosu support in SonarQube! 🎉

**You don't need to be a SonarQube or ANTLR expert.** If you write Gosu, you already know the most
valuable thing: which mistakes show up in real Guidewire code. The grammar, parse tree, test helpers and
CI are in place, and a typical rule is one small class plus a test.

Ways to help, from quickest to most involved:

- ⭐ **Report valid Gosu code that raises a `ParsingError`.** The grammar was tested on the open-source
  gosu-lang sources; your Guidewire code likely uses syntax they don't. A short snippet is enough.
- 💡 **Propose a rule**, even if you don't want to write it. Describe the problem and show bad and good code.
- 🛠️ **Implement a rule** from the list below, or your own.
- 🧩 **Improve the grammar**, metrics or symbol table.

Open an [issue](../../issues/new/choose) to discuss anything before you start. Questions are welcome.

## Rules wanted

Pick one, comment on its issue (or open one) so nobody duplicates work, and go.

**Good first rules**, each can copy an existing rule:

| Rule | Idea | Start from |
|---|---|---|
| Switch without default | `switch` with no `default` case | `EmptyCatchBlockCheck` |
| Collapsible if | `if (a) { if (b) { ... } }` without `else` can be merged | `EmptyCatchBlockCheck` |
| Too many lines in a function | function body longer than N lines (parameter) | `TooManyParametersCheck` |
| Empty function body | function with an empty body and no explaining comment | `EmptyCatchBlockCheck` |
| Boolean literal comparison | `x == true`, `flag != false` | `PrintStatementCheck` |

**Medium:**

| Rule | Idea |
|---|---|
| Unused local variable | declared but never read; `SymbolTableVisitor` already links declarations to uses |
| Unused private function | never called within its class |
| Identical branches | `if` and `else` (or two `case`s) with the same code |
| Deep nesting | `if`/`for`/`while`/`try` nested deeper than N |
| Cognitive complexity | the metric SonarQube uses for "hard to understand" functions |

**Guidewire experts wanted** — these are the rules no other tool has:

| Rule | Idea |
|---|---|
| Query in a loop | `Query.make(...)` or `.select()` inside `for`/`while`: one database round-trip per iteration |
| In-memory filtering of entity arrays | `.where(...)` on an entity array loads every row; filter in the query instead |
| Hard-coded display text | user-facing strings that should be display keys |
| Bundle misuse | modifying entities outside a bundle, or committing inside loops |
| Your team's code review rules | what do reviewers keep flagging in your Guidewire code? |

Careful when porting Java rules: Gosu is not Java. In Gosu, `==` calls `equals()` (`===` compares
identity), there are no checked exceptions, and properties replace getters and setters.

## Your first rule, step by step

Example: flag a `switch` without a `default` case.

**1. Find the grammar rule.** Open [`Gosu.g4`](src/main/antlr4/org/sonargosu/plugin/parser/Gosu.g4)
and search for `switch`:

```antlr
switchStatement : 'switch' '(' expression ')' '{' switchBlockStatementGroup* '}' ;
switchBlockStatementGroup : ('case' expression ':' | 'default' ':') statement* ;
```

ANTLR generates a listener method for each grammar rule (`enterSwitchStatement`) and a context class
with a method per child (`ctx.switchBlockStatementGroup()`).

**2. Write the check** in `src/main/java/org/sonargosu/plugin/checks/SwitchWithoutDefaultCheck.java`:

```java
public class SwitchWithoutDefaultCheck extends TreeCheck {

  @Override
  public void enterSwitchStatement(GosuParser.SwitchStatementContext ctx) {
    boolean hasDefault = ctx.switchBlockStatementGroup().stream()
      .anyMatch(group -> group.getStart().getText().equals("default"));
    if (!hasDefault) {
      report(ctx, "Add a \"default\" case to this switch.");
    }
  }
}
```

`TreeCheck` walks the parse tree for you and skips files that don't parse.
For line or text based rules, implement `GosuCheck` instead and use `GosuFile.lines()` or `tokens()`.

**3. Register it** with one entry in the [`GosuRule`](src/main/java/org/sonargosu/plugin/rules/GosuRule.java)
enum: key, title, HTML description (with a noncompliant and a compliant example), severity, type and
parameters. The rules repository, the quality profile and the sensor all read from that enum.

**4. Test it** in [`ChecksTest`](src/test/java/org/sonargosu/plugin/checks/ChecksTest.java), with code
that should and code that should not raise an issue:

```java
@Test
void switch_without_default() {
  String code = """
    switch (x) { case 1: print("one") }
    switch (x) { case 1: print("one") default: print("other") }
    """;
  assertThat(programIssueLines(new SwitchWithoutDefaultCheck(), code)).containsExactly(1);
}
```

**5. Check for false positives on real code** (see below), then open a pull request.

**Tip:** to see the parse tree of a snippet, install the ANTLR v4 plugin for IntelliJ or VS Code,
open `Gosu.g4`, and preview the `compilationUnit` (classes) or `program` (`.gsp`) rule.

## Building

Requires JDK 17+. Maven is downloaded by the wrapper.

```bash
./mvnw verify        # macOS / Linux
mvnw.cmd verify      # Windows
```

## Testing against real code

Download the [gosu-lang sources](https://github.com/gosu-lang/gosu-lang/archive/refs/heads/master.zip),
unzip them, and run:

```bash
./mvnw test -Dtest=GosuCorpusTest -Dgosu.corpus=/path/to/gosu-lang-master
```

- `target/corpus-report.txt` lists every file that fails to parse, with the first error.
- `target/corpus-issues.txt` lists every issue the rules raise. Skim your new rule's issues for false positives.
- Files named `*Errant*` are compiler test cases meant to be invalid; they are counted separately.

CI runs this on every pull request and requires a 99% parse rate. When you fix a grammar gap, add
the snippet to `GosuParserTest` so it stays fixed.

## Reporting a parsing problem

Open a **"Valid code raises ParsingError"** issue with a small snippet that fails to parse (remove anything
confidential), the error message, and your Gosu or Guidewire version if you know it.

## Pull requests

- Keep each pull request to one rule or one fix.
- Include tests, and make sure `./mvnw verify` passes.
- Mention any false positives you saw on the corpus and how you handled them.

By contributing, you agree that your contribution is licensed under the Apache License 2.0, like the
rest of the project.

## Testing in a real SonarQube

See [docs/local-testing.md](docs/local-testing.md).
