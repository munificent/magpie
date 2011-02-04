package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class NothingParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return Expr.nothing(token.getPosition());
  }
}
