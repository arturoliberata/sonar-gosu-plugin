package org.sonargosu.plugin.lexer;

public enum TokenType {
  KEYWORD,
  IDENTIFIER,
  STRING,
  NUMBER,
  ANNOTATION,
  COMMENT,
  DOC_COMMENT,
  OPERATOR;

  public boolean isComment() {
    return this == COMMENT || this == DOC_COMMENT;
  }
}
