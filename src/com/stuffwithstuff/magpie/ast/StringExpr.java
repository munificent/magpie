package com.stuffwithstuff.magpie.ast;

public class StringExpr extends Expr {
  public StringExpr(String value) {
    mValue = value;
  }
  
  public String getValue() { return mValue; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return mValue; }

  private final String mValue;
}
