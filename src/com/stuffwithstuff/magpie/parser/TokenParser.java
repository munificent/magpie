package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class TokenParser {
  public static class Terminate extends TokenParser {
    @Override
    public int getLeftStickiness() { return 0; }
  }
  
  /*
  public static class Literal extends TokenParser {
    @Override
    public Expr parseBefore(Token token) {
      return Expr.literal(token.getValue());
    }
  }
  
  public static class Add extends TokenParser {
    @Override
    public Expr parseAfter(Repl repl, Expr left) {
      Expr right = repl.parse(10);
      return new Expr.Binary(left, "+", right);
    }
    
    @Override
    public int getLeftStickiness() { return 10; }
  }
  
  public static class Multiply extends TokenParser {
    public Expr parseAfter(Repl repl, Expr left) {
      Expr right = repl.parse(20);
      return new Expr.Binary(left, "*", right);
    }
    
    @Override
    public int getLeftStickiness() { return 20; }
  }
  */
  
  public Expr parseBefore(MagpieParser parser, Token token) {
    throw new RuntimeException("Undefined");
  }
  
  public Expr parseAfter(MagpieParser parser, Expr left) {
    throw new RuntimeException("Missing operator");
  }
  
  public int getLeftStickiness() { return 0; }
}
