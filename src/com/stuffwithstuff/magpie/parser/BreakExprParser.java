package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.BreakExpr;

public class BreakExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.BREAK);
    Position position = parser.last(1).getPosition();
    return new BreakExpr(position);
  }
}
