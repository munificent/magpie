package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ExtendExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume("extend");
    
    // Figure out what we're extending.
    if (parser.match("class")) {
      return ClassExprParser.parseClass(parser, true);
    } else {
      parser.consume("interface");
      return InterfaceExprParser.parseInterface(parser, true);
    }
  }
}
