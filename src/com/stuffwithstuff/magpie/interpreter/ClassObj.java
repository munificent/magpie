package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

/**
 * Object type for an object that represents a class. ClassObj adds another
 * layer of fields and methods beyond what Obj provides. This forms the
 * distinction between instance members and "static" or shared members. If an
 * Obj has a method, that method exists only on that one single instance. If a
 * ClassObj has a method, that method exists only on that one single class
 * instance. (An example here would be a constructor for a specific class.)
 * 
 * If a ClassObj has an *instance* method, then every Obj whose class is that
 * ClassObj has that method. These are what most methods are.
 */
public class ClassObj extends Obj {
  /**
   * Special constructor for the magical "Type" type object. This one is special
   * because it needs to have its type reference point to itself.
   */
  public ClassObj() {
    mName = "Type";
  }
  
  public ClassObj(ClassObj type, String name) {
    super(type);
    mName = name;
  }
  
  public String getName() { return mName; }
  
  public Map<String, Boolean> getInstanceFields() { return mInstanceFields; }
  public Map<String, Method> getInstanceMethods() { return mInstanceMethods; }
  
  public void addInstanceField(String name) {
    mInstanceFields.put(name, true);
  }
  
  public void addInstanceMethod(String name, Method method) {
    mInstanceMethods.put(name, method);
  }
  
  @Override
  public String toString() { return mName; }
  
  private final String mName;
  // TODO(bob): Eventually value should be type declaration.
  private final Map<String, Boolean> mInstanceFields = new HashMap<String, Boolean>();
  private final Map<String, Method> mInstanceMethods = new HashMap<String, Method>();
}
