package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Parses type annotations. Type annotations use a subset of the normal
 * expression grammar and, aside from parsing, are treated just like
 * expressions.
 * 
 * @author bob
 */
public class TypeParser {
  public static Expr parse(MagpieParser parser) {
    return operator(parser);
  }
  
  /**
   * Parses a composite literal: a tuple ("a, b") or a record ("x: 1, y: 2").
   */
  private static Expr composite(MagpieParser parser) {
    if (parser.lookAhead(TokenType.FIELD)) {
      Position position = parser.current().getPosition();
      List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
      do {
        String name = parser.consume(TokenType.FIELD).getString();
        Expr value = operator(parser);
        fields.add(new Pair<String, Expr>(name, value));
      } while (parser.match(TokenType.COMMA));
      
      return Expr.record(position, fields);
    } else {
      List<Expr> fields = new ArrayList<Expr>();
      do {
        fields.add(operator(parser));
      } while (parser.match(TokenType.COMMA));
      
      // Only wrap in a tuple if there are multiple fields.
      if (fields.size() == 1) return fields.get(0);
      
      return Expr.tuple(fields);
    }
  }
  
  /**
   * Parses a series of operator expressions like "a + b - c".
   */
  private static Expr operator(MagpieParser parser) {
    Expr left = message(parser);
    
    while (parser.match(TokenType.OPERATOR)) {
      String op = parser.last(1).getString();
      Expr right = message(parser);

      left = Expr.message(left.getPosition().union(right.getPosition()),
          null, op, Expr.tuple(left, right));
    }
    
    return left;
  }
  
  /**
   * Parse a series of message sends, argument applies, and static argument
   * applies. Basically everything in the core syntax that works left-to-right.
   */
  private static Expr message(MagpieParser parser) {
    Expr message = null;
    
    while (true) {
      if (parser.match(TokenType.NAME)) {
        message = Expr.message(parser.last(1).getPosition(), message,
            parser.last(1).getString());
      } else if (parser.match(TokenType.LEFT_PAREN)) {
        // A function application like foo(123).
        Expr arg = composite(parser);
        parser.consume(TokenType.RIGHT_PAREN);
        
        if (message == null) {
          // No receiver, so it's just parentheses for grouping.
          message = arg;
        } else {
          message = Expr.call(message, arg);
        }
      } else {
        break;
      }
    }
    
    if (message == null) {
      throw new ParseException("Could not parse type expression at " +
          parser.current().getPosition());
    }
    
    return message;
  }
  
  private TypeParser() {}
}
