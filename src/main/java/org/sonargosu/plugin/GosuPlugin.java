package org.sonargosu.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonargosu.plugin.rules.GosuQualityProfile;
import org.sonargosu.plugin.rules.GosuRulesDefinition;

/**
 * Entry point of the plugin: registers every extension SonarQube should load.
 */
public class GosuPlugin implements Plugin {

  public static final String CATEGORY = "Gosu";

  @Override
  public void define(Context context) {
    context.addExtensions(
      GosuLanguage.class,
      GosuRulesDefinition.class,
      GosuQualityProfile.class,
      GosuSensor.class,
      PropertyDefinition.builder(GosuLanguage.FILE_SUFFIXES_KEY)
        .defaultValue(GosuLanguage.DEFAULT_FILE_SUFFIXES)
        .name("File Suffixes")
        .description("List of file suffixes that will be analyzed as Gosu.")
        .category(CATEGORY)
        .multiValues(true)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
  }
}
