package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ConjunctionParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    Expr right = parser.parseExpression(getStickiness());

    if (token.getType() == TokenType.AND) {
      return Expr.and(left, right);
    } else {
      return Expr.or(left, right);
    }
  }
  
  @Override
  public int getStickiness() { return 60; }
}
