package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class FnParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    return parser.parseFunction();
  }
}
