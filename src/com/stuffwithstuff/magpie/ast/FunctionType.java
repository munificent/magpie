package com.stuffwithstuff.magpie.ast;

/**
 * AST node representing a function type declaration, like (a Int -> b String).
 */
public class FunctionType {
  public FunctionType(Expr paramType, Expr returnType) {
    mParamType = paramType;
    mReturnType = returnType;
  }
  
  public Expr getParamType() { return mParamType; }
  public Expr getReturnType() { return mReturnType; }
  
  private final Expr mParamType;
  private final Expr mReturnType;
  
}
