package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public interface InfixParser {
  Expr parse(MagpieParser parser, Expr left, Token token);
  int getPrecedence();
}
