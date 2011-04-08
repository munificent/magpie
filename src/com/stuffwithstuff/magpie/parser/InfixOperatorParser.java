package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * Simple infix parselet that translates an infix operator into a message of
 * the same name, i.e.:
 * 
 * a + b   -->   a +(b)
 * 
 * @author rnystrom
 *
 */
public class InfixOperatorParser implements InfixParser {
  public InfixOperatorParser(int stickiness, boolean isRightAssociative) {
    mStickiness = stickiness;
    mIsRightAssociative = isRightAssociative;
  }
  
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Ignore a newline after the operator.
    parser.match(TokenType.LINE);
    
    Expr right = parser.parseExpression(getStickiness() -
        (mIsRightAssociative ? 1 : 0));
    return Expr.call(token.getPosition(), left, token.getString(), right);
  }
  
  @Override
  public int getStickiness() { return mStickiness; }
  
  private final int mStickiness;
  private final boolean mIsRightAssociative;
}
