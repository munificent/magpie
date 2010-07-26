package com.stuffwithstuff.magpie.ast;

public class NameExpr extends Expr {
  public NameExpr(String name) {
    mName = name;
  }
  
  public String getName() { return mName; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return mName; }

  private final String mName;
}
