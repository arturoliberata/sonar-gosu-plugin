package org.sonargosu.plugin.rules;

import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonargosu.plugin.GosuLanguage;

public class GosuRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "gosu";
  public static final String REPOSITORY_NAME = "Gosu Analyzer";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, GosuLanguage.KEY).setName(REPOSITORY_NAME);
    for (GosuRule rule : GosuRule.values()) {
      NewRule newRule = repository.createRule(rule.key())
        .setName(rule.title())
        .setHtmlDescription(rule.htmlDescription())
        .setSeverity(rule.severity())
        .setType(rule.type());
      for (GosuRule.Param param : rule.params()) {
        newRule.createParam(param.key())
          .setDescription(param.description())
          .setDefaultValue(param.defaultValue())
          .setType(RuleParamType.INTEGER);
      }
    }
    repository.done();
  }
}
