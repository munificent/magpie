package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope and the object that "this" refers to.
 */
public class EvalContext {
  public EvalContext(Scope scope, Obj thisObj) {
    mScope = scope;
    mThis = thisObj;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext pushScope() {
    return new EvalContext(new Scope(mScope), mThis);
  }
  
  /**
   * Creates an EvalContext that discards the current innermost lexical scope.
   */
  public EvalContext popScope() {
    return new EvalContext(mScope.getParent(), mThis);
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference.
   */
  public EvalContext withThis(Obj thisObj) {
    return new EvalContext(mScope, thisObj);
  }
  
  public Scope getScope() { return mScope; }
  public Obj   getThis()  { return mThis; }
  
  /**
   * Looks up the given name in current local scope. Does not walk up the
   * lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUpHere(String name) {
    return mScope.get(name);
  }
  
  /**
   * Looks up the given name in the context's lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUp(String name) {
    // Walk up the scope chain until we find it.
    Scope scope = mScope;
    while (scope != null) {
      Obj value = scope.get(name);
      if (value != null) return value;
      scope = scope.getParent();
    }
    
    return null;
  }

  /**
   * Defines (or redefines) a variable with the given name in this context's
   * current scope.
   * 
   * @param name  Name of the variable to define.
   * @param value The variable's value.
   */
  public void define(String name, Obj value) {
    mScope.define(name, value);
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
    Scope scope = mScope;
    while (scope != null) {
      if (scope.assign(name, value)) return true;
      scope = scope.getParent();
    }
    
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("this -> ").append(mThis).append("\n");
    
    String indent = "";
    Scope scope = mScope;
    while (scope != null) {
      for (Entry<String, Obj> entry : scope.entries()) {
        builder.append(indent).append(entry.getKey())
               .append(" -> ").append(entry.getValue()).append("\n");
      }
      
      indent = indent + "  ";
      scope = scope.getParent();
    }
    
    return builder.toString();
  }
  
  private final Scope mScope;
  private final Obj   mThis;
}
