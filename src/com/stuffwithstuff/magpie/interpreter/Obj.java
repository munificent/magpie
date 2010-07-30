package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;

public class Obj {
  /**
   * Special constructor for the magical "Class" class object. This one is
   * special because it needs to have its class reference point to itself.
   */
  public Obj() {
    mClass = (ClassObj)this;
    mPrimitiveValue = null;
  }
  
  public Obj(ClassObj classObj) {
    mClass = classObj;
    mPrimitiveValue = null;
  }
  
  public Obj(ClassObj classObj, Object primitiveValue) {
    mClass = classObj;
    mPrimitiveValue = primitiveValue;
  }
  
  public ClassObj getClassObj() { return mClass; }
  public Object getPrimitiveValue() { return mPrimitiveValue; }
  public Map<String, Method> getMethods() { return mMethods; }
  
  @Override
  public String toString() {
    if (mPrimitiveValue == null) return "() : " + mClass.toString();
    return mPrimitiveValue.toString() + " : " + mClass.toString();
  }
  
  private final ClassObj mClass;
  private final Object mPrimitiveValue;
  private final Map<String, Method> mMethods = new HashMap<String, Method>();
}
