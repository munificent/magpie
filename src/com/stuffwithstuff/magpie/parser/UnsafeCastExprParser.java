package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.UnsafeCastExpr;

/**
 * Parses an unsafe cast expression of the form:
 * 
 *   unsafecast[Some Type](some value)
 */
public class UnsafeCastExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position pos = parser.consume().getPosition();
    
    // Parse the type.
    parser.consume(TokenType.LEFT_BRACKET);
    Expr type = parser.parseTypeExpression();
    parser.consume(TokenType.RIGHT_BRACKET);
    
    // Parse the value.
    parser.consume(TokenType.LEFT_PAREN);
    Expr value = parser.parseExpression();
    parser.consume(TokenType.RIGHT_PAREN);
    
    pos = pos.union(parser.last(1).getPosition());
    return new UnsafeCastExpr(pos, type, value);
  }
}
