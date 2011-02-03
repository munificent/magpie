package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class BraceParser extends TokenParser {
  @Override
  public Expr parseBefore(MagpieParser parser, Token token) {
    parser.pushQuote();
    Position position = token.getPosition();
    Expr expr = parser.groupExpression(
        TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE, false);
    position = position.union(parser.last(1).getPosition());
    parser.popQuote();
    return Expr.quote(position, expr);
  }
}
