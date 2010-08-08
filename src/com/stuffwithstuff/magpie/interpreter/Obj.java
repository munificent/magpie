package com.stuffwithstuff.magpie.interpreter;


public class Obj {  
  public Obj(ClassObj classObj, Object primitiveValue) {
    mClass = classObj;
    mPrimitiveValue = primitiveValue;
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
    mPrimitiveValue = null;
  }
  
  public ClassObj getClassObj() { return mClass; }
  public Object getPrimitiveValue() { return mPrimitiveValue; }
  
  /**
   * Gets the value of a given field.
   * @param name   The name of the field.
   * @return The value or null if there is no field with that name.
   */
  public Obj getField(String name) {
    return mFields.get(name);
  }
  
  public Obj getTupleField(int index) {
    return getField(Integer.toString(index));
  }

  /**
   * Sets the given field to the given value.
   * @param name   The name of the field.
   * @param member The fields's value.
   */
  public void setField(String name, Obj field) {
    mFields.define(name, field);
  }
  
  public Invokable findMethod(String name) {
    return mClass.findInstanceMethod(name);
  }
  
  public boolean asBool() {
    if (mPrimitiveValue instanceof Boolean) {
      return ((Boolean)mPrimitiveValue).booleanValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a boolean.", this));
  }
  
  public int asInt() {
    if (mPrimitiveValue instanceof Integer) {
      return ((Integer)mPrimitiveValue).intValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not an int.", this));
  }
  
  public String asString() {
    if (mPrimitiveValue instanceof String) {
      return (String)mPrimitiveValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a string.", this));
  }
  
  @Override
  public String toString() {
    // Use the object's name if it has one.
    Obj name = mFields.get("name");
    if (name != null) return name.getPrimitiveValue().toString();
    
    // Else try its value.
    if (mPrimitiveValue == null) return "()";
    return mPrimitiveValue.toString();
  }
  
  private final ClassObj mClass;
  private final Object mPrimitiveValue;
  private final Scope mFields = new Scope();
}
