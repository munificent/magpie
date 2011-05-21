package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ThrowParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    Expr value = parser.parseExpressionOrBlock();
    return Expr.throw_(span.end(), value);
  }
}
