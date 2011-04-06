package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class LiteralParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    switch (token.getType()) {
    case BOOL:
      return Expr.bool(token.getPosition(), token.getBool());
    case INT:
      return Expr.int_(token.getPosition(), token.getInt());
    case NOTHING:
      return Expr.nothing(token.getPosition());
    case STRING:
      return Expr.string(token.getPosition(), token.getString());
    }
    
    throw new ParseException("Unexpected token type for literal.");
  }
}
