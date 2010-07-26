package com.stuffwithstuff.magpie.ast;

public class MethodExpr extends Expr {
  public MethodExpr(Expr receiver, String method, Expr arg) {
    mReceiver = receiver;
    mMethod = method;
    mArg = arg;
  }
  
  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(mReceiver)
           .append(".")
           .append(mMethod)
           .append("(");
    
    if (!(mArg instanceof UnitExpr)) builder.append(mArg);
    
    builder.append(")");
    return builder.toString();
  }
  
  private final Expr mReceiver;
  private final String mMethod;
  private final Expr mArg;
}
