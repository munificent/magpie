package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.TypeofExpr;

public class TypeofExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    Position position = parser.consume(TokenType.TYPEOF).getPosition();
    return new TypeofExpr(position, parser.parseBlock());
  }
}
