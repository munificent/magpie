package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class IntExpr extends Expr {
  IntExpr(Position position, int value) {
    super(position);
    mValue = value;
  }
  
  public int getValue() { return mValue; }
  
  @Override
  public boolean isLiteral() {
    return true;
  }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(mValue);
  }

  private final int mValue;
}
