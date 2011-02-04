package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class InfixParser {
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    throw new RuntimeException("Missing operator");
  }
  
  public int getStickiness() { return 0; }
}
