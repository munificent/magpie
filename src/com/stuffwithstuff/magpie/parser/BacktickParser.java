package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.UnquoteExpr;

public class BacktickParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    if (!parser.inQuotation()) {
      throw new ParseException("Cannot unquote outside of a quotation.");
    }
    
    Position position = token.getPosition();
    Expr body;
    if (parser.match(TokenType.NAME)) {
      body = Expr.message(parser.last(1).getPosition(), null,
          parser.last(1).getString());
    } else {
      parser.consume(TokenType.LEFT_PAREN);
      body = parser.groupExpression(TokenType.RIGHT_PAREN);
    }
    
    return new UnquoteExpr(position, body);
  }
}
