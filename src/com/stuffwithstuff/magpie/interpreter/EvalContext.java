package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope and the object that "this" refers to.
 */
public class EvalContext {
  public EvalContext(Scope scope) {
    mScope = scope;
    mIsInLoop = false;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext pushScope() {
    return new EvalContext(new Scope(mScope), mIsInLoop);
  }
  
  /**
   * Creates an EvalContext that discards the current innermost lexical scope.
   */
  public EvalContext popScope() {
    return new EvalContext(mScope.getParent(), mIsInLoop);
  }

  /**
   * Creates a new EvalContext with the same scope as this one, but inside a
   * loop.
   */
  public EvalContext enterLoop() {
    return new EvalContext(mScope, true);
  }
  
  public Scope   getScope() { return mScope; }

  public boolean isInLoop() { return mIsInLoop; }
  
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
  
  public Obj lookUpHere(String name) {
    return mScope.get(name);
  }

  /**
   * Defines a new variable with the given name in this context's current scope.
   * 
   * @param name  Name of the variable to define.
   * @param value The variable's value.
   * @return      True if it was a new name and was defined, false if there is
   *              already a variable with that name in this scope and no new
   *              definition was created.
   */
  public boolean define(String name, Obj value) {
    if (!mScope.canDefine(name)) return false;
    
    mScope.define(name, value);
    return true;
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
  
  private EvalContext(Scope scope, boolean isInLoop) {
    mScope = scope;
    mIsInLoop = isInLoop;
  }

  private final Scope    mScope;
  private final boolean  mIsInLoop;
}
