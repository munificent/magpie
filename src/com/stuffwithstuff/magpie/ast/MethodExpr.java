package com.stuffwithstuff.magpie.ast;

public class MethodExpr extends Expr {
  public MethodExpr(Expr receiver, String method, Expr arg) {
    mReceiver = receiver;
    mMethod = method;
    mArg = arg;
  }

  public Expr   getReceiver() { return mReceiver; }
  public String getMethod()   { return mMethod; }
  public Expr   getArg()      { return mArg; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

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
