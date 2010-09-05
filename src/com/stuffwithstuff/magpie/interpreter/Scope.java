package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * A scope for named variables. This is used to define the name environment
 * both for local variables in a block, and for members in an object.
 */
public class Scope {
  public Scope(Scope parent) {
    mParent = parent;
  }
  
  public Scope() {
    this(null);
  }
  
  public Scope getParent() {
    return mParent;
  }
  
  public boolean assign(String name, Obj value) {
    Expect.notEmpty(name);
    Expect.notNull(value);
    
    if (!mVariables.containsKey(name)) return false;
    
    mVariables.put(name, value);
    return true;
  }

  public void define(String name, Obj value) {
    Expect.notEmpty(name);
    Expect.notNull(value);
    
    mVariables.put(name, value);
  }

  public Obj get(String name) {
    Expect.notEmpty(name);
    
    return mVariables.get(name);
  }

  public Set<Entry<String, Obj>> entries() {
    return mVariables.entrySet();
  }
  
  private final Scope mParent;
  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
}
