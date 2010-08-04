package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.type.TypeDecl;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(boolean isExtend, String name) {
    mIsExtend = isExtend;
    mName = name;
  }
  
  public boolean isExtend() { return mIsExtend; }
  public String getName() { return mName; }
  
  public void defineConstructor(FnExpr function) {
    mConstructors.add(function);
  }
  
  public void declareField(String name, TypeDecl type) {
    mFieldDeclarations.put(name, type);
  }
  
  public void defineField(boolean isShared, String name, Expr initializer) {
    if (isShared) {
      mSharedFields.put(name, initializer);
    } else {
      mFields.put(name, initializer);
    }
  }
  
  public void defineMethod(boolean isShared, String name, FnExpr method) {
    if (isShared) {
      mSharedMethods.put(name, method);
    } else {
      mMethods.put(name, method);
    }
  }
  
  public List<FnExpr> getConstructors() { return mConstructors; }
  public Map<String, TypeDecl> getFieldDeclarations() { return mFieldDeclarations; }
  public Map<String, Expr> getFields() { return mFields; }
  public Map<String, Expr> getSharedFields() { return mSharedFields; }
  public Map<String, FnExpr> getMethods() { return mMethods; }
  public Map<String, FnExpr> getSharedMethods() { return mSharedMethods; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  private final boolean mIsExtend;
  private final String mName;
  private final List<FnExpr> mConstructors = new ArrayList<FnExpr>();
  private final Map<String, TypeDecl> mFieldDeclarations = new HashMap<String, TypeDecl>();
  private final Map<String, Expr> mFields = new HashMap<String, Expr>();
  private final Map<String, Expr> mSharedFields = new HashMap<String, Expr>();
  private final Map<String, FnExpr> mMethods = new HashMap<String, FnExpr>();
  private final Map<String, FnExpr> mSharedMethods = new HashMap<String, FnExpr>();
}
