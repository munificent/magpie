package com.stuffwithstuff.magpie.interpreter;

import java.util.*;


public class MethodSet {
  
  public void add(String name, Invokable method) {
    // Merge it with the other overloads of the same name.
    Multimethod multimethod = mMethods.get(name);
    if (multimethod == null) {
      multimethod = new Multimethod();
      mMethods.put(name, multimethod);
    }
    
    multimethod.add(method);
  }
  
  public Invokable find(String name, Obj arg) {
    // TODO(bob): Make sure arg type matches.
    Multimethod multimethod = mMethods.get(name);
    if (multimethod == null) return null;
    
    return multimethod.find(arg);
  }
  
  private final Map<String, Multimethod> mMethods = new HashMap<String, Multimethod>();
}
