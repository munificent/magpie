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
  
  public Invokable findInstanceMethod(String name, Obj arg) {
    return mInstanceMethods.find(name, arg);
  }
  
  public void addConstructor(FnObj constructor) {
    mConstructor.add(constructor);
  }

  public Invokable findConstructor(Obj arg) {
    return mConstructor.find(arg);
  }
  
  public void defineFields(Map<String, Expr> fields) {
    mFieldInitializers = fields;
  }
  
  private final Multimethod mConstructor = new Multimethod();
  private Map<String, Expr> mFieldInitializers;
  private final MethodSet mInstanceMethods = new MethodSet();
}
