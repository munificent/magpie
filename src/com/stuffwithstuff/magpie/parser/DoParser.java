package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class DoParser extends TokenParser {
  @Override
  public Expr parseBefore(MagpieParser parser, Token token) {
    Expr body = parser.parseEndBlock();
    return Expr.scope(body);
  }
}
