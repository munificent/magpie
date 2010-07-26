package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

/**
 * Object type for an object that represents a type.
 */
public class TypeObj extends Obj {
  /**
   * Special constructor for the magical "Type" type object. This one is special
   * because it needs to have its type reference point to itself.
   */
  public TypeObj() {
    mName = "Type";
  }
  
  public TypeObj(TypeObj type, String name) {
    super(type);
    mName = name;
  }
  
  public String getName() { return mName; }
  
  public Method getMethod(String name) {
    return mMethods.get(name);
  }
  
  public void addMethod(String name, Method method) {
    mMethods.put(name, method);
  }
  
  @Override
  public String toString() { return mName; }
  
  private final String mName;
  private final Map<String, Method> mMethods = new HashMap<String, Method>();
}
