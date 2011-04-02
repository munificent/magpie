package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class BreakParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return Expr.break_(token.getPosition());
  }
}
