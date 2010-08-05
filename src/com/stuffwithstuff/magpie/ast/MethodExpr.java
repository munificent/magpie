package com.stuffwithstuff.magpie.ast;

public class MethodExpr extends Expr {
  public MethodExpr(Expr receiver, String method, Expr arg) {
    super(Position.union(receiver.getPosition(), arg.getPosition()));
    
    mReceiver = receiver;
    mMethod = method;
    mArg = arg;
  }

  public Expr   getReceiver() { return mReceiver; }
  public String getMethod()   { return mMethod; }
  public Expr   getArg()      { return mArg; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(mReceiver)
           .append(".")
           .append(mMethod)
           .append("(");
    
    if (!(mArg instanceof NothingExpr)) builder.append(mArg);
    
    builder.append(")");
    return builder.toString();
  }
  
  private final Expr mReceiver;
  private final String mMethod;
  private final Expr mArg;
}
