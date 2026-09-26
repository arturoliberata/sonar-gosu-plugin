package org.sonargosu.plugin.parser;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Entry point for parsing Gosu source. Uses the usual two-stage strategy: a fast SLL pass that
 * bails on the first error, then a full LL pass with error recovery only if SLL failed.
 */
public final class GosuParserFacade {

  public enum Kind {
    /** .gs / .gsx files: one class, interface, structure, enum or enhancement. */
    CLASS,
    /** .gsp files: top-level statements and functions. */
    PROGRAM
  }

  private GosuParserFacade() {
  }

  public static Kind kindOf(String filename) {
    return filename.toLowerCase(java.util.Locale.ROOT).endsWith(".gsp") ? Kind.PROGRAM : Kind.CLASS;
  }

  public static GosuParseResult parse(String source, Kind kind) {
    GosuLexer lexer = new GosuLexer(CharStreams.fromString(source));
    lexer.removeErrorListeners();
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    GosuParser parser = new GosuParser(tokens);
    parser.removeErrorListeners();

    parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
    parser.setErrorHandler(new BailErrorStrategy());
    try {
      return new GosuParseResult(entryRule(parser, kind), List.of());
    } catch (ParseCancellationException e) {
      // fall through to the slower, more precise pass
    }

    tokens.seek(0);
    parser.reset();
    List<GosuParseResult.SyntaxError> errors = new ArrayList<>();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int column,
        String msg, RecognitionException e) {
        errors.add(new GosuParseResult.SyntaxError(line, column, msg));
      }
    });
    parser.getInterpreter().setPredictionMode(PredictionMode.LL);
    parser.setErrorHandler(new DefaultErrorStrategy());
    return new GosuParseResult(entryRule(parser, kind), errors);
  }

  private static ParserRuleContext entryRule(GosuParser parser, Kind kind) {
    return kind == Kind.PROGRAM ? parser.program() : parser.compilationUnit();
  }
}
