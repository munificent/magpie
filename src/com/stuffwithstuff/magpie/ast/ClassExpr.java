package com.stuffwithstuff.magpie.ast;

import java.util.*;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(Position position, boolean isExtend, String name) {
    super(position);
    
    mIsExtend = isExtend;
    mName = name;
  }
  
  public boolean isExtend() { return mIsExtend; }
  public String getName() { return mName; }
  
  public void defineConstructor(FnExpr function) {
    mConstructors.add(function);
  }
  
  public void declareField(String name, Expr type) {
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
      addMethod(mSharedMethods, name, method);
    } else {
      addMethod(mMethods, name, method);
    }
  }
  
  public List<FnExpr> getConstructors() { return mConstructors; }
  public Map<String, Expr> getFieldDeclarations() { return mFieldDeclarations; }
  public Map<String, Expr> getFields() { return mFields; }
  public Map<String, Expr> getSharedFields() { return mSharedFields; }
  public Map<String, List<FnExpr>> getMethods() { return mMethods; }
  public Map<String, List<FnExpr>> getSharedMethods() { return mSharedMethods; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  private void addMethod(Map<String, List<FnExpr>> methods, String name,
      FnExpr method) {
    List<FnExpr> collection = methods.get(name);
    if (collection == null) {
      collection = new ArrayList<FnExpr>();
      methods.put(name, collection);
    }
    
    collection.add(method);
  }

  private final boolean mIsExtend;
  private final String mName;
  private final List<FnExpr> mConstructors = new ArrayList<FnExpr>();
  private final Map<String, Expr> mFieldDeclarations = new HashMap<String, Expr>();
  private final Map<String, Expr> mFields = new HashMap<String, Expr>();
  private final Map<String, Expr> mSharedFields = new HashMap<String, Expr>();
  private final Map<String, List<FnExpr>> mMethods = new HashMap<String, List<FnExpr>>();
  private final Map<String, List<FnExpr>> mSharedMethods = new HashMap<String, List<FnExpr>>();
}
