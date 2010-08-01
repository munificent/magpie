package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

/**
 * A scope for named variables. This is used to define the name environment
 * both for local variables in a block, and for members in an object.
 */
public class Scope {
  public boolean assign(String name, Obj value) {
    if (!mVariables.containsKey(name)) return false;
    
    mVariables.put(name, value);
    return true;
  }

  public void define(String name, Obj value) {
    mVariables.put(name, value);
  }
  
  public Obj get(String name) {
    return mVariables.get(name);
  }

  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
}
