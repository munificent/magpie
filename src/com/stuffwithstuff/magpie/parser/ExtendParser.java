package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ExtendParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    parser.consume("class");
    return ClassParser.parseClass(parser, true);
  }
}
