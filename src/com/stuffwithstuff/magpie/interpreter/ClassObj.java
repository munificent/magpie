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
  
  public void addInstanceMember(String name, Obj value) {
    mInstanceMembers.define(name, value);
  }
  
  public Obj getInstanceMember(String name) {
    return mInstanceMembers.get(name);
  }
  
  public void defineFields(Map<String, Expr> fields) {
    mFieldInitializers = fields;
  }
  
  private Map<String, Expr> mFieldInitializers;
  private final Scope mInstanceMembers = new Scope();
}
