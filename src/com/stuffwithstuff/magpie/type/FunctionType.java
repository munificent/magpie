package com.stuffwithstuff.magpie.type;

import java.util.List;

/**
 * AST node representing a function type declaration, like (a Int -> b String).
 */
public class FunctionType extends TypeDecl {
  public FunctionType(TypeDecl paramType, TypeDecl returnType) {
    mParamType = paramType;
    mReturnType = returnType;
  }
  
  public TypeDecl getParamType() { return mParamType; }
  public TypeDecl getReturnType() { return mReturnType; }
  
  private final TypeDecl mParamType;
  private final TypeDecl mReturnType;
  
}
