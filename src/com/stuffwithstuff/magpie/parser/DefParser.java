package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Parses a method definition.
 */
public class DefParser implements PrefixParser {
  public static Pair<String, Pattern> parseSignature(MagpieParser parser) {
    // No receiver:        def print(text String)
    // No arg method:      def (this String) reverse()
    // Shared method:      def (Int) parse(text String)
    // Getter:             def (this String) count
    // Method on anything: def (this) debugDump()
    // Value receiver:     def (true) not()
    // Value arg:          def fib(0)
    // Constant receiver:  def (LEFT_PAREN) not()
    // Constant arg:       def string(LEFT_PAREN)
    // Setter:             def (this Person) name = (name String)
    // Setter with arg:    def (this List) at(index Int) = (item)
    // Complex receiver:   def (a Int, b Int) sum()
    // Indexer:            def (this String)[index Int]
    // Index setter:       def (this String)[index Int] = (c Char)

    // Parse the left argument, if any.
    Pattern leftArg;
    if (parser.lookAhead(TokenType.LEFT_PAREN)) {
      leftArg = parsePattern(parser);
    } else {
      leftArg = Pattern.nothing();
    }
    
    // Parse the message.
    String name;
    Pattern rightArg;
    if (parser.match(TokenType.NAME)) {
      // Regular named message.
      name = parser.last(1).getString();
      
      // Parse the right argument, if any.
      if (parser.lookAhead(TokenType.LEFT_PAREN)) {
        rightArg = parsePattern(parser);
      } else {
        rightArg = null;
      }
    } else {
      // No name, so it must be an indexer.
      name = "[]";
      parser.consume(TokenType.LEFT_BRACKET);
      
      if (!parser.match(TokenType.RIGHT_BRACKET)) {
        rightArg = PatternParser.parse(parser);
        parser.consume(TokenType.RIGHT_BRACKET);
      } else {
        rightArg = Pattern.nothing();
      }
    }
    
    // Parse the setter's rvalue type, if any.
    Pattern setValue = null;
    if (parser.match(TokenType.EQUALS)) {
      setValue = parsePattern(parser);
    }

    // Combine into a single multimethod pattern.
    // def m(r)         -> m(nothing, r)
    // def (l) g        -> g(l)
    // def (l) m(r)     -> m(l, r)
    // def s(r) = v     -> s_=((nothing, r), v)
    // def (l) s = v    -> s_=(l, v)
    // def (l) s(r) = v -> s_=((l, r), v)
    
    Pattern pattern;
    if (rightArg == null) {
      pattern = leftArg;
    } else {
      pattern = Pattern.record(leftArg, rightArg);
    }
    
    if (setValue != null) {
      name = Name.makeAssigner(name);
      pattern = Pattern.record(pattern, setValue);
    }
    
    return new Pair<String, Pattern>(name, pattern);
  }
  
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    Pair<String, Pattern> signature = parseSignature(parser);
    
    // Parse the doc comment if given.
    String doc = "";
    if (parser.match(TokenType.LINE, TokenType.DOC_COMMENT)) {
      doc = parser.last(1).getString();
    }

    if (!parser.lookAhead(TokenType.LINE)) {
      throw new ParseException("A method body must be a block.");
    }
    
    Expr body = parser.parseBlock();
    
    return Expr.method(span.end(), doc, 
        signature.getKey(), signature.getValue(), body);
  }
  
  private static Pattern parsePattern(MagpieParser parser) {
    parser.consume(TokenType.LEFT_PAREN);
    if (parser.match(TokenType.RIGHT_PAREN)) return Pattern.nothing();
    
    Pattern pattern = PatternParser.parse(parser);
    parser.consume(TokenType.RIGHT_PAREN);
    return pattern;
  }
}
