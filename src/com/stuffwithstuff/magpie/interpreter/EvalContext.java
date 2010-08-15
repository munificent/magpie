package com.stuffwithstuff.magpie.interpreter;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope, this reference, etc.
 */
public class EvalContext {
  public EvalContext(Scope scope, Obj thisObj) {
    mScope = scope;
    mThis = thisObj;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext nestScope() {
    return new EvalContext(new Scope(mScope), mThis);
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference.
   */
  public EvalContext withThis(Obj thisObj) {
    return new EvalContext(mScope, thisObj);
  }
  
  public Obj getThis() {
    return mThis;
  }
  
  public Scope getScope() { return mScope; }
  
  public Obj lookUp(String name) {
    Scope scope = mScope;
    while (scope != null) {
      Obj value = scope.get(name);
      if (value != null) return value;
      scope = scope.getParent();
    }
    
    return null;
  }

  public void define(String name, Obj value) {
    mScope.define(name, value);
  }
  
  public boolean assign(String name, Obj value) {
    Scope scope = mScope;
    while (scope != null) {
      if (scope.assign(name, value)) return true;
      scope = scope.getParent();
    }
    
    return false;
  }

  private final Scope mScope;
  private final Obj   mThis;
}
