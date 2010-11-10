package com.stuffwithstuff.magpie.ast;

public class CatchClause {
  public CatchClause(FunctionType type, Expr body) {
    mType = type;
    mBody = body;
  }
  
  public FunctionType getType() { return mType; }
  public Expr         getBody() { return mBody; }
  
  private final FunctionType mType;
  private final Expr         mBody;
}
