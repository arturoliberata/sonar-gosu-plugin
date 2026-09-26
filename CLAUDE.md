# sonar-gosu-plugin

SonarQube plugin for Gosu (Guidewire). Java 17, Maven wrapper, ANTLR 4 grammar. Human docs:
README.md (rules table), CONTRIBUTING.md (rule walkthrough).

## Commands (keep output short)

- One test class: `mvnw.cmd -q test -Dtest=FooCheckTest` (use `./mvnw` on macOS/Linux)
- Everything, once at the end: `mvnw.cmd -q verify`
- Corpus: `mvnw.cmd -q test -Dtest=GosuCorpusTest -Dgosu.corpus=<gosu-lang dir>`. Don't read
  `target/corpus-issues.txt` whole. Grep it for the new rule keys, count per key, read ~10 per key.
- Don't read `Gosu.g4` whole (it's large). Grep for the grammar rule you need.

## Adding a rule: every step

1. `src/main/java/org/sonargosu/plugin/checks/<Key>Check.java`. Extend `TreeCheck` (parse-tree
   listener; skips unparsable files) or implement `GosuCheck` (lines/tokens).
2. One entry in the `GosuRule` enum (`rules/GosuRule.java`): key, title, severity, type, factory, params.
3. `src/main/resources/org/sonargosu/plugin/rules/<Key>.html`: why, noncompliant, compliant, with
   Gosu code. The build fails without it.
4. `src/test/java/org/sonargosu/plugin/checks/<Key>CheckTest.java`, one file per rule, using
   `CheckTestUtils.issueLines` (class file) / `programIssueLines` (.gsp, top-level statements) /
   `issues` (line + message). Copy an existing test file of the same shape.
5. README.md: rule count in Features, and a row in the Rules table.
6. Corpus: look for false positives before calling the rule done.

Sample project (`sample-project/src/acme/*.gs` + `SampleProjectTest`) is optional per rule. Only
add a line there when it demos something unit tests can't. If you do, `SampleProjectTest`
expects exact `Key:line` lists, so recount line numbers after editing a sample file.

## Reusable helpers (read these instead of hunting)

- `TreeCheck`: `report(ctx|terminal, msg)`, `modifiersOf`, `hasModifier`, `hasAnnotation`,
  `isEmptyWithoutComment`, `containsComment`, `startFile()`.
- `GosuFile`: `lines()`, `tokens()`, `codeTokens()`, `tree()` (null if parse errors or `.gst`),
  `symbols()` (declarations + references, `Kind` enum), `nameOccurrences(name)` (counts uses
  inside string templates too), `templateIdentifiers()`.
- `Branches`: if/else-if chains and switch groups. `HardcodedValueCheck`: base for
  name/value-matching rules. `CognitiveComplexityCheck`: how to track nesting.

## Gosu is not Java: rule pitfalls

- `==` calls `equals()`; `===` is identity. String `==` is correct in Gosu.
- No checked exceptions, no C-style `for`, no labels, no multi-catch.
- Assignments and `++` are statements, not expressions.
- Fields are private by default; `var _x : T as X` exposes a property (so it's used).
- `${...}` and `<% %>` templates live inside string tokens: the tree can't see names there, so
  use `GosuFile.nameOccurrences` for "is it used" rules.
- Blocks: `\ x -> expr`. Feature literals: `A#foo()` count as a use.

## Conventions

- Never write secret-looking literals (`AKIA...`, `ghp_...`) in source. Build them at runtime in tests.
- Write source files with an editor/Write tool, not PowerShell `Set-Content` (adds a BOM, breaks javac).
- Keep private notes and paths out of the repo. They go in `CLAUDE.local.md` (git-ignored).
