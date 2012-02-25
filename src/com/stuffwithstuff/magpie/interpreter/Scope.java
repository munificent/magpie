package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * A lexical scope for named variables and multimethods.
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
  
  private Scope(Scope parent) {
    mAllowRedefinition = false;
    mModule = parent.mModule;
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
  
  public Scope push() {
    return new Scope(this);
  }
  
  public void importName(String name, String rename, Module module,
      boolean export) {
    
    // Import variable.
    Obj variable = module.getScope().get(name);
    if (variable != null) {
      if (!mAllowRedefinition && (get(rename) != null)) {
        mModule.error(Name.REDEFINITION_ERROR,
            "Can not import variable \"" + rename + "\" from " +
            module.getName() + " because there is already a variable with " +
            "that name defined.");
      }
      
      mVariables.put(rename, new Pair<Boolean, Obj>(false, variable));
    }
    
    // Import multimethod.
    Multimethod multimethod = module.getScope().getMultimethod(name);
    if (multimethod != null || mAllowRedefinition) {
      Multimethod existing = mMultimethods.get(rename);
      if ((existing != null) && (existing != multimethod)) {
        mModule.error(Name.REDEFINITION_ERROR,
            "Can not import multimethod \"" + rename + "\" from " +
            module.getName() + " because there is already a multimethod with " +
            "that name defined.");
      }
      
      mMultimethods.put(rename, multimethod);
      // TODO(bob): Right now, all top-level multimethods are defined in the
      // global multimethod set, and not in the module itself, so we should
      // never hit this case. Eventually, we do want to support this so that
      // you can have importable non-global methods. (They will work
      // essentially like extension methods in C#.)
      throw new RuntimeException("Untested");
    }
    
    // Re-export.
    if (export && (mParent == null)) {
      mModule.export(name);
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
      Pair<Boolean, Obj> variable = scope.mVariables.get(name);
      if (variable != null) {
        // Only assign if the variable is mutable.
        // TODO(bob): Should be a static error.
        if (variable.getKey()) {
          scope.mVariables.put(name, new Pair<Boolean, Obj>(true, value));
        }
        return true;
      }
      scope = scope.getParent();
    }
    
    return false;
  }

  public boolean define(boolean isMutable, String name, Obj value) {
    Expect.notEmpty(name);
    Expect.notNull(value);

    // Don't allow redefinition.
    if (!mAllowRedefinition && (get(name) != null)) return false;

    mVariables.put(name, new Pair<Boolean, Obj>(isMutable, value));
    
    // If we're defining a top-level public variable, export it too.
    if ((mParent == null) && Name.isPublic(name)) {
      mModule.export(name);
    }
    
    return true;
  }

  public Obj get(String name) {
    Expect.notEmpty(name);
    
    Pair<Boolean, Obj> variable = mVariables.get(name);
    if (variable == null) return null;
    return variable.getValue();
  }
  
  public Multimethod getMultimethod(String name) {
    return mMultimethods.get(name);
  }
  
  public Multimethod define(String name, Callable method) {
    // Define it if not already present.
    Multimethod multimethod = defineMultimethod(name, "");
    
    multimethod.addMethod(method);
    
    return multimethod;
  }
  
  public Multimethod defineMultimethod(String name, String doc) {
    if ((mParent == null) && Name.isPublic(name)) {
      // Top-level public name, so define it globally.
      Multimethod multimethod = mModule.getInterpreter().getMultimethods().get(name);
      
      // Only define it the first time if not found.
      if (multimethod == null) {
        multimethod = new Multimethod(doc);
        mModule.getInterpreter().getMultimethods().put(name, multimethod);
      }
      
      return multimethod;
    }
    
    // Otherwise, it's a local multimethod.
    return getOrCreateMultimethod(name, doc);
  }

  public Multimethod lookUpMultimethod(String name) {
    Scope scope = this;
    
    // Walk up the parent scopes.
    while (scope != null) {
      Multimethod multimethod = scope.mMultimethods.get(name);
      if (multimethod != null) return multimethod;
      scope = scope.mParent;
    }
    
    // It's not a local one, so see if it's global.
    return mModule.getInterpreter().getMultimethods().get(name);
  }

  public Set<Entry<String, Pair<Boolean, Obj>>> entries() {
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
      for (Entry<String, Pair<Boolean, Obj>> entry : scope.entries()) {
        if (entry.getValue().getKey()) {
          builder.append("var ");
        } else {
          builder.append("val ");
        }
        builder.append(entry.getKey())
               .append(" = ").append(entry.getValue().getValue()).append("\n");
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

  private Multimethod getOrCreateMultimethod(String name, String doc) {
    Multimethod multimethod = mMultimethods.get(name);
    
    // Only define it the first time if not found.
    if (multimethod == null) {
      multimethod = new Multimethod(doc);
      mMultimethods.put(name, multimethod);
    }
    
    return multimethod;
  }
  
  private final boolean mAllowRedefinition;
  private final Scope mParent;
  private final Module mModule;
  private final Map<String, Pair<Boolean, Obj> > mVariables = new HashMap<String, Pair<Boolean, Obj>>();
  private final Map<String, Multimethod> mMultimethods = new HashMap<String, Multimethod>();
}
