package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class MessageExpr extends Expr {
  MessageExpr(Position position, Expr receiver, String name, Expr arg) {
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
    
    if (mArg == null) {
      // Do nothing.
    } else if (mArg instanceof NothingExpr) {
      builder.append("()");
    } else if (mArg instanceof TupleExpr) {
      // Don't double (()).
      builder.append(mArg);
    } else {
      builder.append("(").append(mArg).append(")");
    }
  }

  private final Expr mReceiver;
  private final String mName;
  private final Expr mArg;
}
