package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.UnsafeCastExpr;

public class UnsafeCastParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    PositionSpan span = parser.startBefore();
    
    // Parse the type.
    parser.consume(TokenType.LEFT_BRACKET);
    Expr type = parser.parseTypeAnnotation();
    parser.consume(TokenType.RIGHT_BRACKET);

    // Parse the value.
    parser.consume(TokenType.LEFT_PAREN);
    Expr value = parser.parseExpression();
    parser.consume(TokenType.RIGHT_PAREN);
    
    return new UnsafeCastExpr(span.end(), type, value);
  }
}
