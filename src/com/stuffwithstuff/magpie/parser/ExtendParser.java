package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ExtendParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    // Figure out what we're extending.
    if (parser.match("class")) {
      return ClassParser.parseClass(parser, true);
    } else {
      parser.consume("interface");
      return InterfaceParser.parseInterface(parser, true);
    }
  }
}
