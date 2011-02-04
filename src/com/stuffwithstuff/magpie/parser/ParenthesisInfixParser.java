package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ParenthesisInfixParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // A function call like foo(123).
    Expr arg = parser.groupExpression(TokenType.RIGHT_PAREN);
    return Expr.call(left, arg);
  }
  
  @Override
  public int getStickiness() { return 100; }
}
