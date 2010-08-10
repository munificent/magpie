package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class MessageExpr extends Expr {
  public MessageExpr(Position position, Expr receiver, String name, Expr arg) {
    super(position);
    
    mReceiver = receiver;
    mName = name;
    mArg = arg;
  }

  public Expr   getReceiver() { return mReceiver; }
  public String getName()     { return mName; }
  public Expr   getArg()      { return mArg; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(mReceiver)
           .append(" ")
           .append(mName);
    
    if (!(mArg instanceof NothingExpr)) {
      builder.append("(")
             .append(mArg)
             .append(")");
    }
    
    builder.append(")");
    return builder.toString();
  }
  
  private final Expr mReceiver;
  private final String mName;
  private final Expr mArg;
}
