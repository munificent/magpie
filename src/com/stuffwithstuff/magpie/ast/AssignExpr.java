package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class AssignExpr extends Expr {
  public AssignExpr(Position position, Expr receiver, String name, Expr value) {
    super(position);
    
    mReceiver = receiver;
    mName = name;
    mValue = value;
  }
  
  public Expr getReceiver() { return mReceiver; }
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    if (mReceiver != null) mReceiver.toString(builder, indent);
    builder.append(mName).append(" = ");
    mValue.toString(builder, indent);
  }

  private final Expr mReceiver;
  private final String mName;
  private final Expr mValue;
}
