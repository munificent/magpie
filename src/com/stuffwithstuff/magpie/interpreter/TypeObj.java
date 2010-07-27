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
  public TypeObj(int typeId) {
    mTypeId = typeId;
    mName = "Type";
  }
  
  public TypeObj(TypeObj type, int typeId, String name) {
    super(type);
    mTypeId = typeId;
    mName = name;
  }
  
  public String getName() { return mName; }
  
  public Method getMethod(String name, TypeObj argType) {
    // TODO(bob): Should use argType to select from overloaded methods.
    return mMethods.get(name);
  }
  
  public void addMethod(String name, Method method) {
    mMethods.put(name, method);
  }
  
  @Override
  public String toString() { return mName; }
  
  private final int mTypeId;
  private final String mName;
  private final Map<String, Method> mMethods = new HashMap<String, Method>();
}
