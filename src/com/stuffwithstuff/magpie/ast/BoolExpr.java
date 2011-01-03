package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class BoolExpr extends Expr {
  BoolExpr(Position position, boolean value) {
    super(position);
    mValue = value;
  }
  
  public boolean getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(mValue);
  }

  private final boolean mValue;
}
