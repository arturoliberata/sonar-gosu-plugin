# Contributing

Thanks for helping improve Gosu support in SonarQube! Bug reports, grammar fixes and new rules are all welcome.

## Reporting a parsing problem

If the analysis raises a **ParsingError** issue on valid Gosu code, please open an issue with:

- a small snippet that fails to parse (remove anything confidential),
- the error message from the issue, or from the scanner log (`Unable to parse file ...`),
- your Gosu / Guidewire version, if you know it.

## Building

Requires JDK 17+. Maven is downloaded by the wrapper.

```bash
./mvnw verify        # macOS / Linux
mvnw.cmd verify      # Windows
```

## Testing the grammar against real code

Every grammar change should keep real Gosu code parsing. Download the
[gosu-lang sources](https://github.com/gosu-lang/gosu-lang/archive/refs/heads/master.zip), unzip them, and run:

```bash
./mvnw test -Dtest=GosuCorpusTest -Dgosu.corpus=/path/to/gosu-lang-master
```

- `target/corpus-report.txt` lists every file that fails to parse, with the first error.
- `target/corpus-issues.txt` lists every issue the rules raise, useful for spotting false positives.
- Files named `*Errant*` are compiler test cases meant to be invalid; they are counted separately.

CI runs the same test on every pull request and requires a 99% success rate.

When you fix a grammar gap, add the snippet to `GosuParserTest` so it stays fixed.

## Adding a rule

1. Create a class in `src/main/java/org/sonargosu/plugin/checks/`:
   - **Needs code structure:** extend `TreeCheck` and override listener methods generated from the
     grammar, such as `enterCatchClause(GosuParser.CatchClauseContext ctx)`. The method and class names
     come from the rule names in `Gosu.g4`.
   - **Line or text based:** implement `GosuCheck` and use `GosuFile.lines()` / `tokens()`.
2. Add an entry to the `GosuRule` enum: key, title, HTML description, severity, type and parameters.
   The rules repository, the quality profile and the sensor all read from that enum.
3. Add tests to `ChecksTest`, covering both code that should and code that should not raise an issue.
4. Run the corpus test and review `target/corpus-issues.txt` for false positives.

Keep in mind that Gosu is not Java: for example, `==` calls `equals()` in Gosu (`===` compares identity),
and there are no checked exceptions. A Java rule may not carry over as-is.

## Testing in a real SonarQube

See [docs/local-testing.md](docs/local-testing.md).
