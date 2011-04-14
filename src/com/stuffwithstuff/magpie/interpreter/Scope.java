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
    mAllowRedefinition = false;
    mParent = parent;
  }
  
  public Scope(boolean allowRedefinition) {
    mAllowRedefinition = allowRedefinition;
    mParent = null;
  }
  
  public Scope() {
    this(false);
  }
  
  public void importTo(Scope other) {
    // Copy the variables.
    for (Entry<String, Obj> entry : mVariables.entrySet()) {
      other.define(entry.getKey(), entry.getValue());
    }
    
    // Merge the multimethods.
    for (Entry<String, Multimethod> entry : mMultimethods.entrySet()) {
      Multimethod multimethod = Multimethod.define(other, entry.getKey());
      for (Callable method : entry.getValue().getMethods()) {
        multimethod.addMethod(method);
      }
    }
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

  public boolean canDefine(String name) {
    return mAllowRedefinition || (get(name) == null);
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
  
  public void defineMultimethod(String name, Multimethod method) {
    Expect.notEmpty(name);
    Expect.notNull(method);

    mMultimethods.put(name, method);
  }

  public Multimethod getMultimethod(String name) {
    return mMultimethods.get(name);
  }
  
  public Set<Entry<String, Obj>> entries() {
    return mVariables.entrySet();
  }
  
  private final boolean mAllowRedefinition;
  private final Scope mParent;
  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
  private final Map<String, Multimethod> mMultimethods = new HashMap<String, Multimethod>();
}
