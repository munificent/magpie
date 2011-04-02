package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Parses a method definition.
 */
public class DefParser implements PrefixParser {
  public static Pair<String, Pattern> parseSignature(MagpieParser parser) {
    // No receiver:        def print(text String) -> ...
    // No arg method:      def (this String) reverse() -> ...
    // Shared method:      def (Int) parse(text String) -> ...
    // Getter:             def (this String) count -> ...
    // Method on anything: def (this) debugDump() -> ...
    // Value receiver:     def (true) not() -> ...
    // Value arg:          def fib(0) -> ...
    // Constant receiver:  def (LEFT_PAREN) not() -> ...
    // Constant arg:       def string(LEFT_PAREN) -> ...
    // Setter:             def (this Person) name = (name String) -> ...
    // Setter with arg:    def (this List) at(index Int) = (item) -> ...
    // Complex receiver:   def (a Int, b Int) sum() -> ...
    
    // Parse the receiver, if any.
    Pattern receiver;
    if (parser.lookAhead(TokenType.LEFT_PAREN)) {
      receiver = parsePattern(parser);
    } else {
      receiver = Pattern.nothing();
    }
    
    // Parse the message.
    String name = parser.consume(TokenType.NAME).getString();
    
    // Parse the argument, if any.
    Pattern arg;
    if (parser.lookAhead(TokenType.LEFT_PAREN)) {
      arg = parsePattern(parser);
    } else {
      arg = null;
    }
    
    // Parse the setter's rvalue type, if any.
    Pattern setValue = null;
    if (parser.match("=")) {
      setValue = parsePattern(parser);
    }

    // Combine into a single multimethod pattern.
    // def m(a)         -> m(nothing, a)
    // def (r) m(a)     -> m(r, a)
    // def (r) g        -> g(r)
    // def (r) s = v    -> s=(r, v)
    // def (r) s(a) = v -> s=(r, (a, v))
    
    Pattern pattern;
    if (arg == null) {
      if (setValue == null) {
        // Getter.
        pattern = receiver;
      } else {
        // Setter.
        pattern = Pattern.tuple(receiver, setValue);
        name = name + "=";
      }
    } else {
      if (setValue == null) {
        // Method.
        pattern = Pattern.tuple(receiver, arg);
      } else {
        // Setter with argument.
        pattern = Pattern.tuple(receiver, Pattern.tuple(arg, setValue));
        name = name + "=";
      }
    }
    
    return new Pair<String, Pattern>(name, pattern);
  }
  
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    Pair<String, Pattern> signature = parseSignature(parser);

    parser.consume("->");
    Expr body = parser.parseEndBlock();
    
    return Expr.method(span.end(), signature.getKey(), signature.getValue(),
        body);
  }
  
  private static Pattern parsePattern(MagpieParser parser) {
    parser.consume(TokenType.LEFT_PAREN);
    if (parser.match(TokenType.RIGHT_PAREN)) return Pattern.nothing();
    
    Pattern pattern = PatternParser.parse(parser);
    parser.consume(TokenType.RIGHT_PAREN);
    return pattern;
  }
}
