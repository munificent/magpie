package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.NotImplementedException;

/**
 * A scope for named variables. This is used to define the name environment
 * both for local variables in a block, and for members in an object.
 */
public class Scope {
  /**
   * Creates a new top-level scope for the given module.
   * @param module
   */
  public Scope(Module module) {
    mAllowRedefinition = false;
    mModule = module;
    mParent = null;
  }
  
  public Scope(Scope parent) {
    mAllowRedefinition = false;
    mModule = null;
    mParent = parent;
  }
  
  public Scope(boolean allowRedefinition) {
    mAllowRedefinition = allowRedefinition;
    mModule = null;
    mParent = null;
  }
  
  public Scope() {
    this(false);
  }
  
  public void importFrom(Map<String, Obj> variables,
      Map<String, Multimethod> methods) {
    // Copy the variables.
    for (Entry<String, Obj> entry : variables.entrySet()) {
      define(entry.getKey(), entry.getValue());
    }
    
    // Import the multimethods.
    for (Entry<String, Multimethod> entry : methods.entrySet()) {
      if (mMultimethods.containsKey(entry.getKey())) {
        throw new NotImplementedException("Bad! Need to handle multimethod collision!");
      }
      mMultimethods.put(entry.getKey(), entry.getValue());
    }
  }
  
  public Scope getParent() {
    return mParent;
  }
  
  /**
   * Looks up the given name in the context's lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUp(String name) {
    // Walk up the scope chain until we find it.
    Scope scope = this;
    while (scope != null) {
      Obj value = scope.get(name);
      if (value != null) return value;
      scope = scope.getParent();
    }
    
    return null;
  }

  /**
   * Assigns the given value to an existing variable with the given name in the
   * scope where the name is already defined. Does nothing if there is no
   * variable with that name. Note this is different from {code define}, which
   * always binds in the current lexical scope and would shadow an existing
   * definition in an outer scope. This will walk up the scope chain to assign
   * the value wherever it's already defined.
   * 
   * @param name  Name of the variable to assign.
   * @param value The variable's value.
   * @return      True if the variable was found, otherwise false.
   */
  public boolean assign(String name, Obj value) {
    Scope scope = this;
    while (scope != null) {
      if (scope.mVariables.containsKey(name)) {
        scope.mVariables.put(name, value);
        return true;
      }
      scope = scope.getParent();
    }
    
    return false;
  }

  public boolean define(String name, Obj value) {
    Expect.notEmpty(name);
    Expect.notNull(value);

    // Don't allow redefinition.
    if (!mAllowRedefinition && (get(name) != null)) return false;

    mVariables.put(name, value);
    
    // If we're defining a top-level variable, export it too.
    if (mModule != null) {
      mModule.addExport(name, value);
    }
    
    return true;
  }

  public Obj get(String name) {
    Expect.notEmpty(name);
    
    return mVariables.get(name);
  }
  
  public Multimethod defineMultimethod(String name) {
    Multimethod multimethod = mMultimethods.get(name);
    
    // Only define it the first time if not found.
    if (multimethod == null) {
      multimethod = new Multimethod();
      mMultimethods.put(name, multimethod);
      
      // If this is a top-level method, export it too.
      if (mModule != null) {
        mModule.addExport(name, multimethod);
      }
    }
    
    return multimethod;
  }

  public Multimethod lookUpMultimethod(String name) {
    Scope scope = this;
    
    // Walk up the parent scopes.
    while (scope != null) {
      Multimethod multimethod = scope.mMultimethods.get(name);
      if (multimethod != null) return multimethod;
      scope = scope.mParent;
    }
    
    // Not found.
    return null;
  }

  public Set<Entry<String, Obj>> entries() {
    return mVariables.entrySet();
  }
  
  public Map<String, Multimethod> getMultimethods() {
    return mMultimethods;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
    Scope scope = this;
    while (scope != null) {
      for (Entry<String, Obj> entry : scope.entries()) {
        builder.append("var ").append(entry.getKey())
               .append(" = ").append(entry.getValue()).append("\n");
      }
      
      for (Entry<String, Multimethod> multimethod : scope.getMultimethods().entrySet()) {
        builder.append("def ").append(multimethod.getKey()).append("\n");
        for (Callable method : multimethod.getValue().getMethods()) {
          builder.append("    ").append(method.getPattern()).append("\n");
        }
      }
      
      builder.append("----\n");
      scope = scope.getParent();
    }
    
    return builder.toString();
  }

  private final boolean mAllowRedefinition;
  private final Scope mParent;
  private final Module mModule;
  private final Map<String, Obj> mVariables = new HashMap<String, Obj>();
  private final Map<String, Multimethod> mMultimethods = new HashMap<String, Multimethod>();
}
