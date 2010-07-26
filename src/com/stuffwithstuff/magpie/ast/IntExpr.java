package com.stuffwithstuff.magpie.ast;

public class IntExpr extends Expr {
  public IntExpr(int value) {
    mValue = value;
  }
  
  public int getValue() { return mValue; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return Integer.toString(mValue); }

  private final int mValue;
}
