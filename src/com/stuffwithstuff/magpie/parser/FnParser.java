package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class FnParser extends TokenParser {
  @Override
  public Expr parseBefore(MagpieParser parser, Token token) {
    return parser.parseFunction();
  }
}
