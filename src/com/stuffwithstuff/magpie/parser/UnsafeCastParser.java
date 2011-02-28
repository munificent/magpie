package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.UnsafeCastExpr;

public class UnsafeCastParser extends PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    Position position = token.getPosition();
    
    // Parse the type.
    parser.consume(TokenType.LEFT_BRACKET);
    Expr type = parser.parseTypeAnnotation();
    parser.consume(TokenType.RIGHT_BRACKET);

    // Parse the value.
    parser.consume(TokenType.LEFT_PAREN);
    Expr value = parser.parseExpression();
    parser.consume(TokenType.RIGHT_PAREN);
    
    position = position.union(parser.last(1).getPosition());
    return new UnsafeCastExpr(position, type, value);
  }
}
