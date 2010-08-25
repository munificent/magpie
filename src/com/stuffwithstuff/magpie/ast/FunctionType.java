package com.stuffwithstuff.magpie.ast;

import java.util.List;

/**
 * Describes a function's type declaration, including its parameter and return
 * type, along with its parameter names, if any.
 */
public class FunctionType {
  public FunctionType(List<String> paramNames,
      Expr paramType, Expr returnType) {
    mParamNames = paramNames;
    mParamType = paramType;
    mReturnType = returnType;
  }
  
  public List<String> getParamNames() { return mParamNames; }
  public Expr         getParamType()  { return mParamType; }
  public Expr         getReturnType() { return mReturnType; }
  
  private final List<String> mParamNames;
  private final Expr         mParamType;
  private final Expr         mReturnType;
  
}
