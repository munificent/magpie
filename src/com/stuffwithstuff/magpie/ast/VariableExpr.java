package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class VariableExpr extends Expr {
  VariableExpr(Position position, String name, Expr value) {
    super(position);
    mName = name;
    mValue = value;
  }
  
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("var ").append(mName).append(" = ");
    mValue.toString(builder, indent);
  }

  private final String mName;
  private final Expr mValue;
}
