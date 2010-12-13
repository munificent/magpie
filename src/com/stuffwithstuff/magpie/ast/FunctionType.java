package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a function's type declaration, including its parameter and return
 * type, along with its parameter names, if any.
 */
public class FunctionType {
  public static FunctionType nothingToDynamic() {
    return returningType(Expr.name("Dynamic"));
  }
  
  public static FunctionType returningType(Expr type) {
    return new FunctionType(new ArrayList<String>(),
        Expr.name("Nothing"), type, false);
  }
  
  public FunctionType(List<String> paramNames, Expr paramType, Expr returnType,
      boolean isStatic) {
    mParamNames = paramNames;
    mParamType = paramType;
    mReturnType = returnType;
    mIsStatic = isStatic;
  }
  
  public List<String> getParamNames()   { return mParamNames; }
  public Expr         getParamType()    { return mParamType; }
  public Expr         getReturnType()   { return mReturnType; }
  public boolean      isStatic()        { return mIsStatic; }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
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
  
  private final List<String> mParamNames;
  private final Expr         mParamType;
  private final Expr         mReturnType;
  private final boolean      mIsStatic;
  
}
