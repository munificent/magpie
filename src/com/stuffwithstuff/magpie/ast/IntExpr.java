package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Token;

public class IntExpr extends Expr {
  public IntExpr(Token token) {
    super(token.getPosition());
    mValue = token.getInt();
  }
  
  public int getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() { return Integer.toString(mValue); }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(mValue);
  }

  private final int mValue;
}
