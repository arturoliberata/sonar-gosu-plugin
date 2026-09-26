package org.sonargosu.plugin.lexer;

/**
 * A lexical token. Lines are 1-based, columns are 0-based (SonarQube conventions);
 * {@code endColumn} is exclusive.
 */
public record Token(TokenType type, String text, int line, int column, int endLine, int endColumn) {

  public boolean is(String value) {
    return text.equals(value);
  }

  public boolean isKeyword(String keyword) {
    return type == TokenType.KEYWORD && text.equals(keyword);
  }
}
