package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.Position;

public class ReturnExpr extends Expr {
  public ReturnExpr(Position position, Expr value) {
    super(position);
    mValue = value;
  }
  
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return "return " + mValue.toString();
  }

  private final Expr mValue;
}
