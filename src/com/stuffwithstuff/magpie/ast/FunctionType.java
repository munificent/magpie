package com.stuffwithstuff.magpie.ast;

import java.util.Collections;
import java.util.List;

/**
 * Describes a function's type declaration, including its parameter and return
 * type, along with its parameter names, if any.
 */
// TODO(bob): Get rid of all of the static stuff here. Should be handled outside
// of this.
public class FunctionType {
  public FunctionType(List<String> staticParams, List<String> paramNames,
      Expr paramType, Expr returnType) {
    mStaticParams = staticParams;
    mParamNames = paramNames;
    mParamType = paramType;
    mReturnType = returnType;
  }
  
  public FunctionType(List<String> paramNames,
      Expr paramType, Expr returnType) {
    this(Collections.<String>emptyList(), paramNames, paramType, returnType);
  }
  
  public List<String> getStaticParams() { return mStaticParams; }
  public List<String> getParamNames()   { return mParamNames; }
  public Expr         getParamType()    { return mParamType; }
  public Expr         getReturnType()   { return mReturnType; }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
    if (mStaticParams.size() > 0) {
      builder.append("[");
      for (int i = 0; i < mStaticParams.size(); i++) {
        builder.append(mStaticParams.get(i));
        if (i < mStaticParams.size() - 1) {
          builder.append(", ");
        }
      }
      builder.append("]");
    }
    
    TupleExpr params = null;
    if (mParamNames.size() > 1) {
      params = (TupleExpr)mParamType;
    }
    
    builder.append("(");
    for (int i = 0; i < mParamNames.size(); i++) {
      builder.append(mParamNames.get(i)).append(" ");
      if (params != null) {
        builder.append(params.getFields().get(i));
      } else {
        builder.append(mParamType);
      }
      
      if (i < mParamNames.size() - 1) {
        builder.append(", ");
      }
    }
    
    builder.append(" -> ").append(mReturnType).append(")");
    
    return builder.toString();
  }
  
  private final List<String> mStaticParams;
  private final List<String> mParamNames;
  private final Expr         mParamType;
  private final Expr         mReturnType;
  
}
