package com.stuffwithstuff.magpie;

import java.util.*;
import com.stuffwithstuff.magpie.ast.*;

public class MagpieParser extends Parser {
  public MagpieParser(Lexer lexer) {
    super(lexer);
  }
  
  public Expr parse() {
    Expr expr = expression();
    // make sure we didn't stop early
    consume(TokenType.EOF);
    return expr;
  }
  
  private Expr expression() {
    return tuple();
  }
  
  /**
   * Parses a tuple expression like "a, b, c".
   */
  private Expr tuple() {
    List<Expr> fields = new ArrayList<Expr>();
    
    do {
      fields.add(operator());
    } while (match(TokenType.COMMA));
    
    // only wrap in a tuple if there are multiple fields
    if (fields.size() == 1) return fields.get(0);
    
    return new TupleExpr(fields);
  }

  /**
   * Parses a series of operator expressions like "a + b - c".
   */
  private Expr operator() {
    Expr left = method();
    
    while (match(TokenType.OPERATOR)) {
      String op = last(1).getString();
      Expr right = method();
      left = new MethodExpr(left, op, right);
    }
    
    return left;
  }
  
  /**
   * Parses a series of method calls like "list add 123 squared"
   */
  private Expr method() {
    Expr left = call();
    
    while (match(TokenType.NAME)) {
      String method = last(1).getString();
      Expr right;
      try {
        right = call();
      } catch (Error err) {
        // if the argument is omitted, infer ()
        right = new UnitExpr();
      }
      left = new MethodExpr(left, method, right);
    }
    
    return left;
  }
  
  /**
   * Parses a series of non-method calls like "foo(bar)"
   */
  private Expr call() {
    Expr target = primary();
    
    if (match(TokenType.LEFT_PAREN)) {
      Expr arg = expression();
      consume(TokenType.RIGHT_PAREN);
      return new CallExpr(target, arg);
    }
    
    return target;
  }
  
  /**
   * Parses a primary expression like a literal.
   */
  private Expr primary() {
    if (match(TokenType.INT)) {
      return new IntExpr(last(1).getInt());
    } else if (match(TokenType.BOOL)){
      return new BoolExpr(last(1).getBool());
    } else if (match(TokenType.NAME)) {
      return new NameExpr(last(1).getString());
    } else if (match(TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN)) {
      return new UnitExpr();
    } else if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN);
      return expr;
    }
    
    throw new Error("Couldn't parse primary.");
  }
}
