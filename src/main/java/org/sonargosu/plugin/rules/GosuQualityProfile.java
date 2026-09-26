package org.sonargosu.plugin.rules;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonargosu.plugin.GosuLanguage;

/**
 * Built-in "Sonar way" profile for Gosu, activating every rule of the repository.
 */
public class GosuQualityProfile implements BuiltInQualityProfilesDefinition {

  public static final String PROFILE_NAME = "Sonar way";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, GosuLanguage.KEY);
    profile.setDefault(true);
    for (GosuRule rule : GosuRule.values()) {
      profile.activateRule(GosuRulesDefinition.REPOSITORY_KEY, rule.key());
    }
    profile.done();
  }
}
