package org.sonargosu.plugin.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hand-written, error-tolerant lexer for Gosu. It never throws on malformed input:
 * unknown characters become single-character OPERATOR tokens and unterminated
 * strings/comments stop at end of line/file.
 */
public final class GosuLexer {

  public static final Set<String> KEYWORDS = Set.of(
    "abstract", "and", "as", "block", "break", "case", "catch", "class", "construct",
    "continue", "default", "delegate", "do", "else", "enhancement", "enum", "eval",
    "extends", "false", "final", "finally", "for", "foreach", "function", "hide", "if",
    "implements", "in", "interface", "internal", "new", "not", "null", "or", "override",
    "package", "private", "property", "protected", "public", "readonly", "represents",
    "return", "static", "statictypeof", "structure", "super", "switch", "this", "throw",
    "transient", "true", "try", "typeas", "typeis", "typeof", "unless", "uses", "using",
    "var", "void", "while");

  /** Multi-character operators, longest first. {@code >>} is deliberately absent so generics close cleanly. */
  private static final String[] OPERATORS = {
    "|..|", "===", "!==", "<<=", "..|", "|..",
    "&&", "||", "==", "!=", "<=", ">=", "->", "?.", "?:", "?[", "..", ":=",
    "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "++", "--", "<<"
  };

  private final String src;
  private int pos;
  private int line = 1;
  private int column;
  private final List<Token> tokens = new ArrayList<>();

  private GosuLexer(String src) {
    this.src = src;
  }

  public static List<Token> tokenize(String source) {
    GosuLexer lexer = new GosuLexer(source);
    lexer.run();
    return lexer.tokens;
  }

  private void run() {
    while (pos < src.length()) {
      char c = src.charAt(pos);
      if (Character.isWhitespace(c)) {
        advance();
      } else if (c == '/' && peek(1) == '/') {
        lineComment();
      } else if (c == '/' && peek(1) == '*') {
        blockComment();
      } else if (c == '"' || c == '\'') {
        string(c);
      } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
        number();
      } else if (c == '@' && Character.isJavaIdentifierStart(peek(1))) {
        annotation();
      } else if (Character.isJavaIdentifierStart(c)) {
        identifier();
      } else {
        operator();
      }
    }
  }

  private void lineComment() {
    int startPos = pos, startLine = line, startCol = column;
    while (pos < src.length() && !isNewline(src.charAt(pos))) {
      advance();
    }
    emit(TokenType.COMMENT, startPos, startLine, startCol);
  }

  private void blockComment() {
    int startPos = pos, startLine = line, startCol = column;
    boolean doc = peek(2) == '*' && peek(3) != '/';
    advance();
    advance();
    while (pos < src.length() && !(src.charAt(pos) == '*' && peek(1) == '/')) {
      advance();
    }
    if (pos < src.length()) {
      advance();
      advance();
    }
    emit(doc ? TokenType.DOC_COMMENT : TokenType.COMMENT, startPos, startLine, startCol);
  }

  private void string(char quote) {
    int startPos = pos, startLine = line, startCol = column;
    advance();
    while (pos < src.length()) {
      char c = src.charAt(pos);
      if (c == '\\' && pos + 1 < src.length()) {
        // escape sequence, or a backslash before a line break continuing the string
        advance();
        boolean crlf = src.charAt(pos) == '\r' && peek(1) == '\n';
        advance();
        if (crlf) {
          advance();
        }
      } else if (c == quote) {
        advance();
        break;
      } else if (isNewline(c)) {
        break; // unterminated string: stop at end of line
      } else {
        advance();
      }
    }
    emit(TokenType.STRING, startPos, startLine, startCol);
  }

  private void number() {
    int startPos = pos, startLine = line, startCol = column;
    while (pos < src.length()) {
      char c = src.charAt(pos);
      if (Character.isLetterOrDigit(c) || c == '_') {
        advance();
      } else if (c == '.' && Character.isDigit(peek(1))) {
        advance(); // decimal point, but not the ".." range operator
      } else if ((c == '+' || c == '-') && (prev() == 'e' || prev() == 'E') && !isHex(startPos)) {
        advance(); // exponent sign
      } else {
        break;
      }
    }
    emit(TokenType.NUMBER, startPos, startLine, startCol);
  }

  private void annotation() {
    int startPos = pos, startLine = line, startCol = column;
    advance();
    while (pos < src.length() && (Character.isJavaIdentifierPart(src.charAt(pos)) || src.charAt(pos) == '.')) {
      advance();
    }
    emit(TokenType.ANNOTATION, startPos, startLine, startCol);
  }

  private void identifier() {
    int startPos = pos, startLine = line, startCol = column;
    while (pos < src.length() && Character.isJavaIdentifierPart(src.charAt(pos))) {
      advance();
    }
    String word = src.substring(startPos, pos);
    emit(KEYWORDS.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER, startPos, startLine, startCol);
  }

  private void operator() {
    int startPos = pos, startLine = line, startCol = column;
    int length = 1;
    for (String op : OPERATORS) {
      if (src.startsWith(op, pos)) {
        length = op.length();
        break;
      }
    }
    for (int i = 0; i < length; i++) {
      advance();
    }
    emit(TokenType.OPERATOR, startPos, startLine, startCol);
  }

  private void emit(TokenType type, int startPos, int startLine, int startCol) {
    tokens.add(new Token(type, src.substring(startPos, pos), startLine, startCol, line, column));
  }

  private void advance() {
    char c = src.charAt(pos++);
    if (c == '\n' || (c == '\r' && (pos >= src.length() || src.charAt(pos) != '\n'))) {
      line++;
      column = 0;
    } else if (c != '\r') {
      column++;
    }
  }

  private char peek(int offset) {
    int i = pos + offset;
    return i < src.length() ? src.charAt(i) : '\0';
  }

  private char prev() {
    return pos > 0 ? src.charAt(pos - 1) : '\0';
  }

  private boolean isHex(int startPos) {
    return src.startsWith("0x", startPos) || src.startsWith("0X", startPos);
  }

  private static boolean isNewline(char c) {
    return c == '\n' || c == '\r';
  }
}
