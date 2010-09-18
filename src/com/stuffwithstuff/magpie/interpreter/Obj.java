package com.stuffwithstuff.magpie.interpreter;

import java.util.List;


public class Obj {  
  public Obj(ClassObj classObj, Object value) {
    mClass = classObj;
    mValue = value;
  }
  
  public Obj(ClassObj classObj) {
    this(classObj, null);
  }
  
  /**
   * This gets called in one of two contexts. It will either be instantiating
   * the ClassObj that represents ths class of classes, or it will be a
   * NativeMethodObj. For the former, it is its own class, for the latter it
   * will have no class at all.
   */
  public Obj() {
    // If we are a class, we're our own class.
    mClass = (this instanceof ClassObj) ? (ClassObj)this : null;
    mValue = null;
  }
  
  public ClassObj getClassObj() { return mClass; }
  
  /**
   * Gets the value of a given field.
   * @param name   The name of the field.
   * @return The value or null if there is no field with that name.
   */
  public Obj getField(String name) {
    return mFields.get(name);
  }
  
  public Obj getTupleField(int index) {
    return getField("_" + Integer.toString(index));
  }

  /**
   * Sets the given field to the given value.
   * @param name   The name of the field.
   * @param member The fields's value.
   */
  public void setField(String name, Obj field) {
    mFields.define(name, field);
  }
  
  public Object getValue() {
    return mValue;
  }
  
  public void setValue(Object value) {
    mValue = value;
  }
  
  @SuppressWarnings("unchecked")
  public List<Obj> asArray() {
    if (mValue instanceof List<?>) {
      return (List<Obj>)mValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not an array.", this));
  }

  public boolean asBool() {
    if (mValue instanceof Boolean) {
      return ((Boolean)mValue).booleanValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a boolean.", this));
  }
  
  public int asInt() {
    if (mValue instanceof Integer) {
      return ((Integer)mValue).intValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not an int.", this));
  }
  
  public String asString() {
    if (mValue instanceof String) {
      return (String)mValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a string.", this));
  }
  
  @Override
  public String toString() {
    if (mValue instanceof String) {
      return "\"" + mValue + "\"";
    } else if (mValue != null) {
      return mValue.toString();
    } else if (mClass.getName().equals("Nothing")) {
      return "nothing";
    }

    return "Instance of " + mClass.getName();
  }
  
  private final ClassObj mClass;
  private Object mValue;
  private final Scope mFields = new Scope();
}
