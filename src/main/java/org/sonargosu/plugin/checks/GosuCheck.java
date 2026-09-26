package org.sonargosu.plugin.checks;

import java.util.Map;

/**
 * A rule implementation. One instance is created per analysis, configured with the
 * rule's active parameters, then asked to scan every file.
 */
public interface GosuCheck {

  /** Called once with the parameters of the active rule (defaults already applied). */
  default void configure(Map<String, String> params) {
  }

  void scan(GosuFile file, IssueCollector issues);

  @FunctionalInterface
  interface IssueCollector {
    void report(int line, String message);
  }
}
