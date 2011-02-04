package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class BraceParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    parser.pushQuote();
    Position position = token.getPosition();
    Expr expr = parser.groupExpression(TokenType.RIGHT_BRACE);
    position = position.union(parser.last(1).getPosition());
    parser.popQuote();
    return Expr.quote(position, expr);
  }
}
