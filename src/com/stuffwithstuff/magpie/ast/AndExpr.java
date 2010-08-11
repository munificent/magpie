package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class AndExpr extends Expr {
  public AndExpr(Position position, Expr left, Expr right) {
    super(position);
    mLeft = left;
    mRight = right;
  }
  
  public Expr getLeft() { return mLeft; }
  public Expr getRight() { return mRight; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return mLeft.toString() + " and " + mRight.toString();
  }

  private final Expr mLeft;
  private final Expr mRight;
}
