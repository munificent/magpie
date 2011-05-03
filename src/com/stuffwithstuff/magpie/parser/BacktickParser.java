package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class BacktickParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    if (!parser.inQuote()) {
      throw new ParseException("Cannot unquote outside of a quotation.");
    }
    
    Position position = token.getPosition();
    Expr body;
    if (parser.match(TokenType.NAME)) {
      body = Expr.name(parser.last(1).getPosition(),
          parser.last(1).getString());
    } else {
      parser.consume(TokenType.LEFT_PAREN);
      body = parser.groupExpression(TokenType.RIGHT_PAREN);
    }
    
    return Expr.unquote(position, body);
  }
}
