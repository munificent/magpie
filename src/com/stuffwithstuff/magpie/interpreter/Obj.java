package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Obj {
  public Obj() {
    mParent = null;
    mPrimitiveValue = null;
  }
  
  protected Obj(Obj parent) {
    mParent = parent;
    mPrimitiveValue = null;
  }
  
  protected Obj(Obj parent, Object primitiveValue) {
    mParent = parent;
    mPrimitiveValue = primitiveValue;
  }
  
  /**
   * Creates a new Obj with this one as its parent.
   */
  public Obj spawn() { return new Obj(this); }
  
  /**
   * Creates a new Obj with this one as its parent and with the given primitive
   * value.
   */
  public Obj spawn(Object primitiveValue) { return new Obj(this, primitiveValue); }
  
  public Obj getParent() { return mParent; }
  public Object getPrimitiveValue() { return mPrimitiveValue; }

  // TODO(bob): Right now, fields and methods are in distinct namespaces.
  //            Semantically, I don't think I want them to work that way. Might
  //            need to make the add methods check their counterpart for a name
  //            collision.
  
  public Obj getField(String name) {
    // Walk up the parent chain.
    Obj obj = this;
    while (obj != null) {
      Obj field = obj.mFields.get(name);
      if (field != null) return field;
      obj = obj.getParent();
    }
    
    return null;
  }
  
  public Invokable getMethod(String name) {
    // Walk up the parent chain.
    Obj obj = this;
    while (obj != null) {
      Invokable method = obj.mMethods.get(name);
      if (method != null) return method;
      obj = obj.getParent();
    }
    
    return null;
  }
  
  /**
   * Adds the given field to the Obj.
   * @param name  The name of the field.
   * @param field The field's value.
   */
  public void add(String name, Obj field) {
    mFields.put(name, field);
  }
  
  /**
   * Adds the given method to the Obj.
   * @param name   The name of the method.
   * @param method The method.
   */
  public void add(String name, Invokable method) {
    mMethods.put(name, method);
  }
  
  public Map<String, Obj> getFields() { return mFields; }
  public Map<String, Invokable> getMethods() { return mMethods; }

  @Override
  public String toString() {
    // Use the object's name if it has one.
    if (mFields.containsKey("name")) {
      return mFields.get("name").getPrimitiveValue().toString();
    }
    
    // Else try its value.
    if (mPrimitiveValue == null) return "()";
    return mPrimitiveValue.toString();
  }
  
  private final Obj mParent;
  private final Object mPrimitiveValue;
  private final Map<String, Obj> mFields = new HashMap<String, Obj>();
  private final Map<String, Invokable> mMethods = new HashMap<String, Invokable>();
}
