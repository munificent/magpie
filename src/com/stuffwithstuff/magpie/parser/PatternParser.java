package com.stuffwithstuff.magpie.parser;

import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

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
    return record(parser);
  }
  
  private static Pattern record(MagpieParser parser) {
    Map<String, Pattern> fields = new HashMap<String, Pattern>();
    
    int index = 0;
    boolean isRecord = false;
    Pattern field = null;
    do {
      String name;
      if (parser.match(TokenType.FIELD)) {
        name = parser.last(1).getString();
        isRecord = true;
      } else {
        name = Name.getTupleField(index);
      }
      index++;
      
      field = primary(parser);
      fields.put(name, field);
    } while (parser.match(TokenType.COMMA));
    
    // TODO(bob): This code is ugly.
    // If it's just a single pattern with no field name, just return it.
    if (fields.size() == 1 && !isRecord) {
      return field;
    }
    
    return Pattern.record(fields);
  }
  
  private static Pattern primary(MagpieParser parser) {
    if (parser.match("is")) {
      Expr type = parser.parsePrecedence(Precedence.COMPARISON);
      return Pattern.type(type);
    } else if (parser.match("==")) {
      Expr value = parser.parsePrecedence(Precedence.COMPARISON);
      return Pattern.value(value);
    } if (parser.match(TokenType.BOOL)) {
      return Pattern.value(Expr.bool(parser.last(1).getBool()));
    } else if (parser.match(TokenType.INT)) {
      return Pattern.value(Expr.int_(parser.last(1).getInt()));
    } else if (parser.match(TokenType.NOTHING)) {
      return Pattern.value(Expr.nothing(parser.last(1).getPosition()));
    } else if (parser.match(TokenType.STRING)) {
      return Pattern.value(Expr.string(parser.last(1).getString()));
    } else if (parser.match(TokenType.LEFT_PAREN)) {
      // Nested pattern.
      Pattern inner = record(parser);
      
      if (inner == null) {
        throw new ParseException(parser.current().getPosition(),
            "Could not parse primary pattern.");
      }

      parser.consume(TokenType.RIGHT_PAREN);
      return inner;
    } else if (parser.match(TokenType.NAME)) {
      String name = parser.last(1).getString();
      if (name.equals("_")) {
        return Pattern.wildcard();
      } else {
        // Variable pattern, see if it has a pattern after it.
        Pattern pattern = primary(parser);
        if (pattern == null) {
          pattern = Pattern.wildcard();
        }
        return Pattern.variable(name, pattern);
      }
    }
    
    return null;
  }
  
  private PatternParser() {}
}
