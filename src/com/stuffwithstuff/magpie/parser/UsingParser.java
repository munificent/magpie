package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class UsingParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    Token nameToken = parser.parseName();
    return Expr.using(span.end(), nameToken.getString());
  }
}
