package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class FnExprParser implements ExprParser {  
  @Override
  public Expr parse(MagpieParser parser) {
    parser.consume(TokenType.FN);
    return parser.parseFunction();
  }
}
