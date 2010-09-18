package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class MessageExpr extends Expr {
  public MessageExpr(Position position, Expr receiver, String name, Expr arg) {
    super(position);
    
    mReceiver = receiver;
    mName = name;
    mArg = arg;
  }

  public Expr   getReceiver()  { return mReceiver; }
  public String getName()      { return mName; }
  public Expr   getArg()       { return mArg; }
  
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
    
    if (mArg != null) {
      builder.append("(");
      if (!(mArg instanceof NothingExpr)) {
        mArg.toString(builder, indent);
      }
      builder.append(")");
    }
  }

  private final Expr mReceiver;
  private final String mName;
  private final Expr mArg;
}
