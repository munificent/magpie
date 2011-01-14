package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.ast.pattern.WildcardPattern;

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
    return parseTuplePattern(parser);
  }
  
  private static Pattern parseTuplePattern(MagpieParser parser) {
    List<Pattern> patterns = new ArrayList<Pattern>();
    do {
      patterns.add(parsePrimaryPattern(parser));
    } while(parser.match(TokenType.COMMA));
    
    if (patterns.size() == 1) return patterns.get(0);
    return new TuplePattern(patterns);
  }
  
  private static Pattern parsePrimaryPattern(MagpieParser parser) {
    // See if there is a binding for the pattern.
    String name = null;
    if (parser.current().getType() == TokenType.NAME) {
      String token = parser.current().getString();
      if (token.equals("_") ||
          Character.isLowerCase(token.charAt(0))) {
        name = parser.consume().getString();
      }
    }
    
    // Parse the pattern itself.
    Pattern pattern = null;
    if (parser.match(TokenType.BOOL)) {
      pattern = new ValuePattern(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      pattern = new ValuePattern(Expr.int_(parser.last(1).getInt()));
    } else if (parser.match(TokenType.STRING)) {
      pattern = new ValuePattern(Expr.string(parser.last(1).getString()));
    } else if (parser.match(TokenType.LEFT_PAREN)) {
      // TODO(bob): This may not be ideal. This means that:
      // foo (A => B)
      // will be parsed as VarPattern("foo", ValuePattern(A => B))
      // but this:
      // foo A => B
      // will be parsed as VarPattern("foo", TypePattern(A => B))
      pattern = parse(parser);
      parser.consume(TokenType.RIGHT_PAREN);
    } else if (parser.lookAhead(TokenType.NAME)) {
      Expr expr = parser.parseTypeExpression();
      
      // The rule is, a name (or _) before a pattern indicates that it matches
      // against the value's type. Otherwise it does a straight equality test
      // against the value.
      if (name != null) {
        pattern = new TypePattern(expr);
      } else {
        pattern = new ValuePattern(expr);
      }
    }
    
    if (name == null) {
      if (pattern == null) {
        throw new ParseException("Could not parse pattern.");
      }
      
      return pattern;
    }
    
    if (name.equals("_")) {
      if (pattern == null) return new WildcardPattern();
      return pattern;
    }
    
    return new VariablePattern(name, pattern);
  }
  
  private PatternParser() {}
}
