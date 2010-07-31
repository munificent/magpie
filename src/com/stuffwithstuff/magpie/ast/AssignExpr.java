package com.stuffwithstuff.magpie.ast;

public class AssignExpr extends Expr {
  public AssignExpr(String name, Expr value) {
    mName = name;
    mValue = value;
  }
  
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return mName + " = " + mValue;
  }

  private final String mName;
  private final Expr mValue;
}
