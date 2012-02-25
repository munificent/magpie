package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * Parses an infix operator call that just desugars to a regular method call.
 */
public class InfixOperatorParser implements InfixParser {
  public InfixOperatorParser(int precedence) {
    mPrecedence = precedence;
  }
  
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Assume left associativity.
    Expr right = parser.parsePrecedence(mPrecedence);
    
    return Expr.call(left.getPosition().union(right.getPosition()),
        left, token.getText(), right);
  }
  
  @Override
  public int getPrecedence() { return mPrecedence; }
  
  private final int mPrecedence;
}
