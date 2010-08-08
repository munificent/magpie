package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  public ClassObj(ClassObj metaclass, String name, ClassObj parent) {
    super(metaclass);
    mName = name;
    mParent = parent;
  }

  public ClassObj(String name, ClassObj parent) {
    mName = name;
    mParent = parent;
  }

  public Map<String, Expr> getFieldInitializers() {
    return mFieldInitializers;
  }

  public Obj instantiate() {
    return new Obj(this);
  }

  public Obj instantiate(Object primitiveValue) {
    return new Obj(this, primitiveValue);
  }
  
  public String getName() { return mName; }
  
  public ClassObj getParent() { return mParent; }
  
  public void setParent(ClassObj parent) {
    mParent = parent;
  }
  
  public void addMethod(String name, Invokable method) {
    mMethods.add(name, method);
  }
  
  public Map<String, Invokable> getMethods() {
    return mMethods.getMethods();
  }
  
  public Invokable findMethod(String name) {
    // Walk up the inheritance chain.
    ClassObj classObj = this;
    while (classObj != null) {
      Invokable method = classObj.mMethods.find(name);
      if (method != null) return method;
      classObj = classObj.mParent;
    }
    
    // If we got here, it wasn't found.
    return null;
  }
  
  public void addConstructor(FnObj constructor) {
    if (mConstructor != null) {
      throw new InterpreterException("Cannot overload constructors.");
    }
    
    mConstructor = constructor;
  }

  public Invokable getConstructor() {
    return mConstructor;
  }
  
  public void defineFields(Map<String, Expr> fields) {
    mFieldInitializers = fields;
  }
  
  private final String mName;
  private ClassObj mParent;
  private Invokable mConstructor;
  private Map<String, Expr> mFieldInitializers;
  private final MethodSet mMethods = new MethodSet();
}
