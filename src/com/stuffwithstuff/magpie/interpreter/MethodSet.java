package com.stuffwithstuff.magpie.interpreter;

import java.util.*;


public class MethodSet {
  
  public void add(String name, Callable method) {
    if (mMethods.containsKey(name)) {
      throw new InterpreterException("There is already a method named \"" + name + "\".");
    }
    
    mMethods.put(name, method);
  }
  
  public Callable find(String name) {
    return mMethods.get(name);
  }
  
  public Map<String, Callable> getMethods() {
    return mMethods;
  }
  
  private final Map<String, Callable> mMethods = new HashMap<String, Callable>();
}
