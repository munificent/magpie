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
  
  public void addMethod(String name, Callable method) {
    mMethods.add(name, method);
  }
  
  public Map<String, Callable> getMethods() {
    return mMethods.getMethods();
  }
  
  public Callable findMethod(String name) {
    // Walk up the inheritance chain.
    ClassObj classObj = this;
    while (classObj != null) {
      Callable method = classObj.mMethods.find(name);
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

  public Callable getConstructor() {
    return mConstructor;
  }
  
  public void defineFields(Map<String, Expr> fields) {
    mFieldInitializers = fields;
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private final String mName;
  private ClassObj mParent;
  private Callable mConstructor;
  private Map<String, Expr> mFieldInitializers;
  private final MethodSet mMethods = new MethodSet();
}
