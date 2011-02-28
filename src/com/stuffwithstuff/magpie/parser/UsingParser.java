package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class UsingParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    Token nameToken = parser.parseName();
    return Expr.using(position.union(nameToken.getPosition()),
        nameToken.getString());
  }
}
