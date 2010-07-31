package com.stuffwithstuff.magpie.ast;

public class NothingExpr extends Expr {
  public NothingExpr() {
  }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() { return "()"; }
}
