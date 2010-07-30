package com.stuffwithstuff.magpie.ast;

public class NothingExpr extends Expr {
  public NothingExpr() {
  }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() { return "()"; }
}
