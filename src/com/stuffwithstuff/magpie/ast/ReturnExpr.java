package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;

public class ReturnExpr extends Expr {
  public ReturnExpr(Position position, Expr value) {
    super(position);
    Expect.notNull(value);    
    
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

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("return ");
    mValue.toString(builder, indent);
  }

  private final Expr mValue;
}
