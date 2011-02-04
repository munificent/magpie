package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ThisParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return Expr.this_(token.getPosition());
  }
}
