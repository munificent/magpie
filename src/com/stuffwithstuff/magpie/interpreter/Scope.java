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

  public Set<Entry<String, Obj>> entries() {
    return mVariables.entrySet();
  }
  
  public void useNamespace(String name) {
    mNamespaces.add(name);
  }

  public Iterable<String> getNamespaces() {
    List<String> namespaces = new ArrayList<String>();
    
    // Check the namespaces in reverse order that they were added, and walking
    // up from child to parent scope.
    Scope scope = this;
    while (scope != null) {
      // Iterate in reverse order so that the last added namespace gets
      // searched first.
      for (int i = scope.mNamespaces.size() - 1; i >= 0; i--) {
        namespaces.add(scope.mNamespaces.get(i));
      }
      scope = scope.mParent;
    }
    
    return namespaces;
  }
  
  private final boolean mAllowRedefinition;
  private final Scope mParent;
  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
  private final List<String> mNamespaces = new ArrayList<String>();
}
