package org.sonargosu.plugin.lexer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GosuLexerTest {

  @Test
  void tokenizes_basic_declaration() {
    List<Token> tokens = GosuLexer.tokenize("var x : int = 42 // answer");
    assertThat(tokens).extracting(Token::type).containsExactly(
      TokenType.KEYWORD, TokenType.IDENTIFIER, TokenType.OPERATOR, TokenType.IDENTIFIER,
      TokenType.OPERATOR, TokenType.NUMBER, TokenType.COMMENT);
    assertThat(tokens.get(6).text()).isEqualTo("// answer");
  }

  @Test
  void tracks_positions_across_lines() {
    List<Token> tokens = GosuLexer.tokenize("/* a\r\n b */\nfoo");
    Token comment = tokens.get(0);
    assertThat(comment.line()).isEqualTo(1);
    assertThat(comment.endLine()).isEqualTo(2);
    assertThat(comment.endColumn()).isEqualTo(5);
    Token foo = tokens.get(1);
    assertThat(foo.line()).isEqualTo(3);
    assertThat(foo.column()).isZero();
    assertThat(foo.endColumn()).isEqualTo(3);
  }

  @Test
  void handles_strings_with_escapes_and_templates() {
    List<Token> tokens = GosuLexer.tokenize("\"a \\\" ${b} c\" 'd'");
    assertThat(tokens).extracting(Token::type).containsExactly(TokenType.STRING, TokenType.STRING);
  }

  @Test
  void range_operator_is_not_part_of_number() {
    List<Token> tokens = GosuLexer.tokenize("for (i in 1..10)");
    assertThat(tokens).extracting(Token::text).containsExactly("for", "(", "i", "in", "1", "..", "10", ")");
  }

  @Test
  void recognizes_doc_comments_and_annotations() {
    List<Token> tokens = GosuLexer.tokenize("/** doc */ @Deprecated /**/");
    assertThat(tokens).extracting(Token::type)
      .containsExactly(TokenType.DOC_COMMENT, TokenType.ANNOTATION, TokenType.COMMENT);
  }

  @Test
  void tolerates_unterminated_constructs() {
    assertThat(GosuLexer.tokenize("\"open\nnext")).extracting(Token::text).containsExactly("\"open", "next");
    assertThat(GosuLexer.tokenize("/* never closed")).hasSize(1);
  }
}
