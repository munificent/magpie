package com.stuffwithstuff.magpie.interpreter;

public class Obj {
  /**
   * Special constructor for the magical "Type" type object. This one is special
   * because it needs to have its type reference point to itself.
   */
  public Obj() {
    mType = (TypeObj)this;
    mPrimitiveValue = null;
  }
  
  public Obj(TypeObj type) {
    mType = type;
    mPrimitiveValue = null;
  }
  
  public Obj(TypeObj type, Object primitiveValue) {
    mType = type;
    mPrimitiveValue = primitiveValue;
  }
  
  public TypeObj getType() { return mType; }
  public Object getPrimitiveValue() { return mPrimitiveValue; }
  
  @Override
  public String toString() {
    if (mPrimitiveValue == null) return "() : " + mType.toString();
    return mPrimitiveValue.toString() + " : " + mType.toString();
  }
  
  private final TypeObj mType;
  private final Object mPrimitiveValue;
}
