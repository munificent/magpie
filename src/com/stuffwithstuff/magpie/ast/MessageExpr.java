package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class MessageExpr extends Expr {
  MessageExpr(Position position, Expr receiver, String name) {
    super(position);
    
    mReceiver = receiver;
    mName = name;
  }

  public Expr   getReceiver()  { return mReceiver; }
  public String getName()      { return mName; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    if (mReceiver != null) {
      mReceiver.toString(builder, indent);
      builder.append(" ");
    }
    
    builder.append(mName);
  }

  private final Expr mReceiver;
  private final String mName;
}
