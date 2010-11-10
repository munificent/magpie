package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ScopeExpr;

public class DoExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.DO).getPosition();
    return new ScopeExpr(parser.parseBlock());
  }
}
