package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ExtendExprParser implements ExprParser {
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.EXTEND);
    
    // Figure out what we're extending.
    if (parser.match(TokenType.CLASS)) {
      return ClassExprParser.parseClass(parser, true);
    } else {
      parser.consume(TokenType.INTERFACE);
      return InterfaceExprParser.parseInterface(parser, true);
    }
  }
}
