package org.sonargosu.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.server.RulesDefinitionContext;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonargosu.plugin.rules.GosuQualityProfile;
import org.sonargosu.plugin.rules.GosuRule;
import org.sonargosu.plugin.rules.GosuRulesDefinition;

class GosuPluginTest {

  @Test
  void registers_extensions() {
    Plugin.Context context = new PluginContextImpl.Builder()
      .setSonarRuntime(SonarRuntimeImpl.forSonarQube(Version.create(9, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY))
      .build();
    new GosuPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(5);
  }

  @Test
  void language_uses_configured_suffixes() {
    MapSettings settings = new MapSettings();
    assertThat(new GosuLanguage(settings.asConfig()).getFileSuffixes()).containsExactly(".gs", ".gsx", ".gst", ".gsp");
    settings.setProperty(GosuLanguage.FILE_SUFFIXES_KEY, ".gs");
    assertThat(new GosuLanguage(settings.asConfig()).getFileSuffixes()).containsExactly(".gs");
  }

  @Test
  void defines_all_rules() {
    RulesDefinition.Context context = new RulesDefinitionContext();
    new GosuRulesDefinition().define(context);
    RulesDefinition.Repository repository = context.repository(GosuRulesDefinition.REPOSITORY_KEY);
    assertThat(repository.rules()).hasSize(GosuRule.values().length);
    assertThat(repository.rule("LineLength").param("maximumLineLength").defaultValue()).isEqualTo("120");
  }

  @Test
  void profile_activates_all_rules() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    new GosuQualityProfile().define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile =
      context.profile(GosuLanguage.KEY, GosuQualityProfile.PROFILE_NAME);
    assertThat(profile.isDefault()).isTrue();
    assertThat(profile.rules()).hasSize(GosuRule.values().length);
  }
}
