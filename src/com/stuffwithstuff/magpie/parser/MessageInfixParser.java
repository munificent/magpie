package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class MessageInfixParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    if (token.getString().equals("do")) {
      System.out.println();
    }
    return Expr.message(token.getPosition(), left, token.getString());
  }
  
  @Override
  public int getStickiness() { return 100; }
}
