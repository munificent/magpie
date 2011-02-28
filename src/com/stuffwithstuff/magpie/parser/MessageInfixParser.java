package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class MessageInfixParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Parse the whole fully-qualified name.
    Token fullName = parser.parseName(true);
    return Expr.message(fullName.getPosition(), left, fullName.getString());
  }
  
  @Override
  public int getStickiness() { return 100; }
}
