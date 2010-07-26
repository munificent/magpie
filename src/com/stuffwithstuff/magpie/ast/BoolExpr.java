package com.stuffwithstuff.magpie.ast;

public class BoolExpr extends Expr {
  public BoolExpr(boolean value) {
    mValue = value;
  }
  
  public boolean getValue() { return mValue; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return Boolean.toString(mValue); }

  private final boolean mValue;
}
