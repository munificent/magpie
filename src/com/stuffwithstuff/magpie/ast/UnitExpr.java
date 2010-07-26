package com.stuffwithstuff.magpie.ast;

public class UnitExpr extends Expr {
  public UnitExpr() {
  }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return "()"; }
}
