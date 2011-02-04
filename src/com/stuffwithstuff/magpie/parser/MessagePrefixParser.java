package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class MessagePrefixParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return Expr.name(token.getPosition(), token.getString());
  }
}
