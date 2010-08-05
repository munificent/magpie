package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.Token;

public class BoolExpr extends Expr {
  public BoolExpr(Token token) {
    super(token.getPosition());
    mValue = token.getBool();
  }
  
  public boolean getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() { return Boolean.toString(mValue); }

  private final boolean mValue;
}
