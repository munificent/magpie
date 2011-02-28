package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class UsingExpr extends Expr {
  UsingExpr(Position position, String name) {
    super(position);
    mName = name;
  }
  
  public String getName() { return mName; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("using ").append(mName);
  }

  private final String mName;
}
