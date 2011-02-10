package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class OperatorParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    String op = token.getString();
    Expr right = parser.parseExpression(getStickiness());

    return Expr.message(token.getPosition(), null, op, Expr.tuple(left, right));
  }
  
  @Override
  public int getStickiness() { return 80; }
}
