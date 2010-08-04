package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

public class MethodSet {
  
  public void add(String name, Invokable method) {
    if (mMethods.containsKey(name)) throw new UnsupportedOperationException(
        "Overloaded methods are not supported yet.");
    
    mMethods.put(name, method);
  }
  
  public Invokable find(String name, Obj arg) {
    // TODO(bob): Make sure arg type matches.
    return mMethods.get(name);
  }
  
  private final Map<String, Invokable> mMethods = new HashMap<String, Invokable>();
}
