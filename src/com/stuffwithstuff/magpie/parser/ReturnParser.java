package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ReturnParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.span();
    Expr value;
    if (parser.lookAheadAny(TokenType.LINE, TokenType.RIGHT_PAREN,
        TokenType.RIGHT_BRACKET, TokenType.RIGHT_BRACE)) {
      // A return with nothing after it implicitly returns nothing.
      value = Expr.nothing(token.getPosition());
    } else {
      value = parser.parseExpression();
    }
    
    return Expr.return_(span.end(), value);
  }
}
