package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  public ClassObj(ClassObj classClass) {
    super(classClass);
  }

  public ClassObj() {
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
  
  public void addInstanceMethod(String name, Invokable method) {
    mInstanceMethods.add(name, method);
  }
  
  public Map<String, Invokable> getMethods() {
    return mInstanceMethods.getMethods();
  }
  
  public Invokable findInstanceMethod(String name) {
    return mInstanceMethods.find(name);
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
  
  private Invokable mConstructor;
  private Map<String, Expr> mFieldInitializers;
  private final MethodSet mInstanceMethods = new MethodSet();
}
