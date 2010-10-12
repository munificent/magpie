package com.stuffwithstuff.magpie.interpreter;

import java.util.*;


public class MethodSet {
  
  public void add(String name, Callable method) {
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
