package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

/**
 * A lexical scope for named variables. Maintains a stack of nested scopes to
 * support proper block scoping.
 */
public class Scope {
  public Scope() {
    mParent = null;
  }
  
  public Scope(Scope parent) {
    mParent = parent;
  }

  public boolean assign(String name, Obj value) {
    // Walk up the scope chain until we find where it was previously defined.
    Scope scope = this;
    while (scope != null) {
      if (scope.mVariables.containsKey(name)) {
        scope.mVariables.put(name, value);
        return true;
      }
      scope = scope.mParent;
    }
    
    // If we got here, it wasn't defined.
    return false;
  }

  public void define(String name, Obj value) {
    mVariables.put(name, value);
  }
  
  public Obj get(String name) {
    // Walk up the scope chain.
    Scope scope = this;
    while (scope != null) {
      Obj value = scope.mVariables.get(name);
      if (value != null) return value;
      scope = scope.mParent;
    }
    
    // If we got here, it wasn't defined.
    return null;
  }

  private final Scope mParent;
  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
}
