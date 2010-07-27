package com.stuffwithstuff.magpie.ast;

public class AssignExpr extends Expr {
  public AssignExpr(String name, Expr value) {
    mName = name;
    mValue = value;
  }
  
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() {
    return mName + " = " + mValue;
  }

  private final String mName;
  private final Expr mValue;
}
