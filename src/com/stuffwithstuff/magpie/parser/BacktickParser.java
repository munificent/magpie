package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.UnquoteExpr;

public class BacktickParser extends TokenParser {
  @Override
  public Expr parseBefore(MagpieParser parser, Token token) {
    if (!parser.inQuotation()) {
      throw new ParseException("Cannot unquote outside of a quotation.");
    }
    
    Position position = token.getPosition();
    Expr body;
    if (parser.match(TokenType.NAME)) {
      body = Expr.message(parser.last(1).getPosition(), null,
          parser.last(1).getString());
    } else {
      body = parser.groupExpression(
          TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN, true);
    }
    
    return new UnquoteExpr(position, body);
  }
}
