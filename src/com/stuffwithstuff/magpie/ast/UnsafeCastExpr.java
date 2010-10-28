package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class UnsafeCastExpr extends Expr {
  public UnsafeCastExpr(Position position, Expr type, Expr value) {
    super(position);
    mType = type;
    mValue = value;
  }
  
  public Expr getType() { return mType; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("unsafecast[");
    mType.toString(builder, indent);
    builder.append("](");
    mValue.toString(builder, indent);
    builder.append(")");
  }
  
  private final Expr mType;
  private final Expr mValue;
}
