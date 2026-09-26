package org.sonargosu.plugin;

import java.util.Arrays;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class GosuLanguage extends AbstractLanguage {

  public static final String KEY = "gosu";
  public static final String NAME = "Gosu";
  public static final String FILE_SUFFIXES_KEY = "sonar.gosu.file.suffixes";
  /** .gs = class, .gsx = enhancement, .gst = template, .gsp = program */
  public static final String DEFAULT_FILE_SUFFIXES = ".gs,.gsx,.gst,.gsp";

  private final Configuration configuration;

  public GosuLanguage(Configuration configuration) {
    super(KEY, NAME);
    this.configuration = configuration;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = Arrays.stream(configuration.getStringArray(FILE_SUFFIXES_KEY))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toArray(String[]::new);
    return suffixes.length > 0 ? suffixes : DEFAULT_FILE_SUFFIXES.split(",");
  }
}
