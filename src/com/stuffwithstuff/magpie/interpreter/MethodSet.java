package com.stuffwithstuff.magpie.interpreter;

import java.util.*;


public class MethodSet {
  
  public void add(String name, Invokable method) {
    if (mMethods.containsKey(name)) {
      throw new InterpreterException("There is already a method named \"" + name + "\".");
    }
    
    mMethods.put(name, method);
  }
  
  public Invokable find(String name) {
    return mMethods.get(name);
  }
  
  private final Map<String, Invokable> mMethods = new HashMap<String, Invokable>();
}
