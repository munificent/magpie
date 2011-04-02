package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class DoParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Expr body = parser.parseExpression();
    return Expr.scope(body);
  }
}
