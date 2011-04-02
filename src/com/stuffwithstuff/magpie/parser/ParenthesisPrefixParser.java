package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ParenthesisPrefixParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Expr expr = parser.parseExpression();
    parser.consume(TokenType.RIGHT_PAREN);
    
    return expr;
  }
}
