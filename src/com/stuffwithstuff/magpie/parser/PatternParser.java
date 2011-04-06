package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Parses patterns. Patterns are used for match cases, function parameter
 * declarations, and catch clauses.
 * 
 * @author bob
 */
public class PatternParser {
  /**
   * A pattern may have a name, no name, or a wildcard name.
   * It may also have a further pattern expression, or not.
   * Each of those combinations has a different interpretation:
   *
   * no name,  no pattern -> Oops, error!
   * no name,  pattern    -> Just use the pattern.
   * name,     no pattern -> Straight variable pattern.
   * name,     pattern    -> Variable pattern with embedded pattern.
   * wildcard, no pattern -> Wildcard pattern.
   * wildcard, pattern    -> Use the pattern.
   *
   * The last case is a bit special since the wildcard is there but doesn't
   * affect the pattern. It's used to distinguish matching on the value's
   * *type* versus matching the value as *equal to a type*. For example:
   *
   * match foo
   *     case Int   then print("foo is the Int class object")
   *     case _ Int then print("foo's type is Int")
   * end
   * 
   * @param parser
   * @return
   */
  public static Pattern parse(MagpieParser parser) {
    return composite(parser);
  }
  
  private static Pattern composite(MagpieParser parser) {
    if (parser.lookAhead(TokenType.FIELD)) {
      List<Pair<String, Pattern>> fields = new ArrayList<Pair<String, Pattern>>();
      do {
        String name = parser.consume(TokenType.FIELD).getString();
        Pattern value = variable(parser);
        fields.add(new Pair<String, Pattern>(name, value));
      } while (parser.match(TokenType.COMMA));
      
      return Pattern.record(fields);
    } else {
      List<Pattern> patterns = new ArrayList<Pattern>();
      do {
        patterns.add(variable(parser));
      } while(parser.match(TokenType.COMMA));
      
      // Only wrap in a tuple if there are multiple fields.
      if (patterns.size() == 1) return patterns.get(0);
      return Pattern.tuple(patterns);
    }
  }
  
  private static Pattern variable(MagpieParser parser) {
    // See if there is a binding for the pattern.
    String name = null;
    if (parser.current().getType() == TokenType.NAME) {
      String token = parser.current().getString();
      if (token.equals("_") ||
          Character.isLowerCase(token.charAt(0))) {
        name = parser.parseName().getString();
      }
    }
    
    // If we don't have a name, it must be a primary pattern.
    if (name == null) {
      return primary(parser);
    }
    
    // See if there is a type for the variable.
    Expr type = null;
    if (parser.match(TokenType.NAME)) {
      type = Expr.name(parser.last(1).getString());
    }
    
    return Pattern.variable(name, type);
  }
  
  private static Pattern primary(MagpieParser parser) {
    if (parser.match(TokenType.BOOL)) {
      return Pattern.value(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      return Pattern.value(Expr.int_(parser.last(1).getInt()));
    } else if (parser.match(TokenType.NOTHING)) {
      return Pattern.value(Expr.nothing(parser.last(1).getPosition()));
    } else if (parser.match(TokenType.STRING)) {
      return Pattern.value(Expr.string(parser.last(1).getString()));
    } else if (parser.match(TokenType.NAME)) {
      return Pattern.value(Expr.name(parser.last(1).getString()));
    } else if (parser.match(TokenType.LEFT_PAREN)) {
      // Nested pattern.
      Pattern inner = composite(parser);
      parser.consume(TokenType.RIGHT_PAREN);
      return inner;
    }
    
    // TODO(bob): Figure out how constants should be handled.
    
    // Must just be a value.
    Expr expr = parser.parseTypeAnnotation();
    return Pattern.value(expr);
  }
  
  private PatternParser() {}
}
