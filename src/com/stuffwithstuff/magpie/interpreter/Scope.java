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
  // TODO(bob): Remove module from this. It's only needed to export stuff and
  // the few places that do that should already have access to the module.
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
  
  public Module getModule() {
    return mModule;
  }
  
  public void importAll(Context context, String prefix, Module module) {
    // Copy the variables.
    for (Entry<String, Obj> entry : module.getExportedVariables().entrySet()) {
      importVariable(context, prefix + entry.getKey(), entry.getValue(),
          module);
    }
    
    // Import the multimethods.
    for (Entry<String, Multimethod> entry : module.getExportedMultimethods().entrySet()) {
      String name = prefix + entry.getKey();
      importMultimethod(context, name, entry.getValue(), module);
    }
  }

  public void importName(Context context, String name, String rename,
      Module module) {
    
    Obj variable = module.getExportedVariables().get(name);
    if (variable != null) {
      importVariable(context, rename, variable, module);
    }
    
    Multimethod multimethod = module.getExportedMultimethods().get(name);
    if (multimethod != null) {
      importMultimethod(context, rename, multimethod, module);
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
      mModule.addExport(name, value);
    }
    
    return true;
  }

  public Obj get(String name) {
    Expect.notEmpty(name);
    
    Pair<Boolean, Obj> variable = mVariables.get(name);
    if (variable == null) return null;
    return variable.getValue();
  }
  
  public void define(String name, Callable method) {
    // Define it if not already present.
    Multimethod multimethod = defineMultimethod(name);
    
    multimethod.addMethod(method);
  }
  
  public Multimethod defineMultimethod(String name) {
    Multimethod multimethod = mMultimethods.get(name);
    
    // Only define it the first time if not found.
    if (multimethod == null) {
      multimethod = new Multimethod();
      mMultimethods.put(name, multimethod);
    }
    
    // If this is a top-level public method, export it too. Note that we do
    // this outside of the above if() so that if you import a multimethod
    // (which does not add it to the exports) and then add a new method to it,
    // then that multimethod now becomes exported. That satisfies the user's
    // expectation that all top-level defs are exported.
    if ((mParent == null) && Name.isPublic(name)) {
      mModule.addExport(name, multimethod);
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
  
  private void importVariable(Context context, String name, Obj value,
      Module module) {
    if (!mAllowRedefinition && (get(name) != null)) {
      context.error(Name.REDEFINITION_ERROR,
          "Can not import variable \"" + name + "\" from " +
          module.getName() + " because there is already a variable with " +
          "that name defined.");
    }
    
    mVariables.put(name, new Pair<Boolean, Obj>(false, value));
  }
  
  private void importMultimethod(Context context, String name,
      Multimethod multimethod, Module module) {
    if (mAllowRedefinition) return;

    Multimethod existing = mMultimethods.get(name);
    if ((existing != null) && (existing != multimethod)) {
      context.error(Name.REDEFINITION_ERROR,
          "Can not import multimethod \"" + name + "\" from " +
          module.getName() + " because there is already a multimethod with " +
          "that name defined.");
    }
    
    mMultimethods.put(name, multimethod);
  }

  private final boolean mAllowRedefinition;
  private final Scope mParent;
  private final Module mModule;
  private final Map<String, Pair<Boolean, Obj> > mVariables = new HashMap<String, Pair<Boolean, Obj>>();
  private final Map<String, Multimethod> mMultimethods = new HashMap<String, Multimethod>();
}
