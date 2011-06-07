package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class StringExpr extends Expr {
  StringExpr(Position position, String text) {
    super(position);
    mValue = text;
  }
  
  public String getValue() { return mValue; }
  
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
    builder.append("\"").append(mValue).append("\"");
  }

  private final String mValue;
}
