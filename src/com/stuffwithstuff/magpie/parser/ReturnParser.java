package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ReturnExpr;

public class ReturnParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    Expr value;
    if (parser.lookAheadAny(TokenType.LINE, TokenType.RIGHT_PAREN,
        TokenType.RIGHT_BRACKET, TokenType.RIGHT_BRACE)) {
      // A return with nothing after it implicitly returns nothing.
      value = Expr.nothing(token.getPosition());
    } else {
      value = parser.parseExpression();
    }
    
    return new ReturnExpr(span.end(), value);
  }
}
