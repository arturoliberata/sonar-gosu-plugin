package org.sonargosu.plugin.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonargosu.plugin.GosuLanguage;
import org.sonargosu.plugin.GosuSensor;
import org.sonargosu.plugin.rules.GosuRule;
import org.sonargosu.plugin.rules.GosuRulesDefinition;

/**
 * Parses a directory of real Gosu code (step 3 of SonarQube's "supporting new languages" guide).
 * Run with: mvnw test -Dtest=GosuCorpusTest -Dgosu.corpus=C:\path\to\gosu\files
 * Writes a per-file report to target/corpus-report.txt.
 *
 * Files whose name contains "Errant" are compiler test cases that are meant to be invalid,
 * so they are counted separately and never fail the test.
 */
@EnabledIfSystemProperty(named = "gosu.corpus", matches = ".+")
class GosuCorpusTest {

  private static final double MIN_SUCCESS_RATE = Double.parseDouble(System.getProperty("gosu.corpus.minRate", "0"));

  @Test
  void parses_corpus() throws IOException {
    Path root = Paths.get(System.getProperty("gosu.corpus"));
    List<Path> files;
    try (Stream<Path> walk = Files.walk(root)) {
      files = walk.filter(Files::isRegularFile)
        .filter(p -> p.toString().matches("(?i).*\\.(gs|gsx|gsp)$"))
        .sorted()
        .toList();
    }

    int ok = 0;
    int failed = 0;
    int errantOk = 0;
    int errantFailed = 0;
    List<String> report = new ArrayList<>();
    long start = System.nanoTime();
    for (Path file : files) {
      String name = file.getFileName().toString();
      boolean errant = name.contains("Errant");
      String source = Files.readString(file, StandardCharsets.UTF_8).replace("\uFEFF", "");
      GosuParseResult result;
      try {
        result = GosuParserFacade.parse(source, GosuParserFacade.kindOf(name));
      } catch (RuntimeException e) {
        report.add("CRASH\t" + name + "\t" + e);
        failed++;
        continue;
      }
      if (result.hasErrors()) {
        GosuParseResult.SyntaxError first = result.errors().get(0);
        report.add((errant ? "ERRANT\t" : "FAIL\t") + name + "\t" + first.line() + ":" + first.column() + "\t" + first.message());
        if (errant) {
          errantFailed++;
        } else {
          failed++;
        }
      } else if (errant) {
        errantOk++;
      } else {
        ok++;
      }
    }
    long millis = (System.nanoTime() - start) / 1_000_000;

    double rate = ok + failed == 0 ? 1 : (double) ok / (ok + failed);
    String summary = String.format(Locale.ROOT,
      "Parsed %d files in %d ms: %d ok, %d failed (%.1f%% success). Errant test files: %d rejected, %d accepted.",
      files.size(), millis, ok, failed, rate * 100, errantFailed, errantOk);
    report.add(0, summary);
    Files.createDirectories(Paths.get("target"));
    Files.write(Paths.get("target", "corpus-report.txt"), report, StandardCharsets.UTF_8);
    System.out.println(summary);

    assertThat(rate).as(summary).isGreaterThanOrEqualTo(MIN_SUCCESS_RATE);
  }

  /** Runs the whole sensor (highlighting, CPD, metrics, symbols, all rules) on every file. */
  @Test
  void sensor_analyzes_every_corpus_file(@TempDir Path workDir) throws IOException {
    Path root = Paths.get(System.getProperty("gosu.corpus"));
    SensorContextTester context = SensorContextTester.create(root);
    ActiveRulesBuilder rules = new ActiveRulesBuilder();
    for (GosuRule rule : GosuRule.values()) {
      rules.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of(GosuRulesDefinition.REPOSITORY_KEY, rule.key())).build());
    }
    context.setActiveRules(rules.build());

    List<InputFile> inputFiles = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      for (Path file : walk.filter(p -> p.toString().matches("(?i).*\\.(gs|gsx|gsp|gst)$")).toList()) {
        InputFile inputFile = TestInputFileBuilder.create("corpus", root.toFile(), file.toFile())
          .setLanguage(GosuLanguage.KEY)
          .setCharset(StandardCharsets.UTF_8)
          .setContents(Files.readString(file, StandardCharsets.UTF_8).replace("﻿", ""))
          .build();
        context.fileSystem().add(inputFile);
        inputFiles.add(inputFile);
      }
    }
    FileLinesContextFactory fileLines = mock(FileLinesContextFactory.class);
    when(fileLines.createFor(any(InputFile.class))).thenReturn(mock(FileLinesContext.class));

    new GosuSensor(fileLines).execute(context);

    // The sensor logs and skips a file whose analysis throws; such a file has no measures at all.
    List<String> notAnalyzed = inputFiles.stream()
      .filter(f -> context.measure(f.key(), CoreMetrics.NCLOC) == null)
      .map(InputFile::filename)
      .toList();
    System.out.println("Sensor: " + inputFiles.size() + " files, " + context.allIssues().size() + " issues, "
      + notAnalyzed.size() + " not analyzed");
    List<String> issueReport = context.allIssues().stream()
      .map(i -> i.primaryLocation().inputComponent() + ":" + i.primaryLocation().textRange().start().line()
        + "\t" + i.ruleKey().rule() + "\t" + i.primaryLocation().message())
      .sorted()
      .toList();
    Files.createDirectories(Paths.get("target"));
    Files.write(Paths.get("target", "corpus-issues.txt"), issueReport, StandardCharsets.UTF_8);
    assertThat(notAnalyzed).isEmpty();
  }
}
