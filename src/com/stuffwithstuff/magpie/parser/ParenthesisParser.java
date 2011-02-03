package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ParenthesisParser extends TokenParser {
  @Override
  public Expr parseBefore(MagpieParser parser, Token token) {
    Expr expr = parser.parseExpression();
    parser.consume(TokenType.RIGHT_PAREN);
    return expr;
  }
}
