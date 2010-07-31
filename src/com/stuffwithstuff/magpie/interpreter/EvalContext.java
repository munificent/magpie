package com.stuffwithstuff.magpie.interpreter;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope, this reference, etc.
 */
public class EvalContext {
  public EvalContext(Scope scope, Obj thisObj) {
    mScope = scope;
    mThis  = thisObj;
  }
  
  public EvalContext bindThis(Obj thisObj) {
    return new EvalContext(mScope, thisObj);
  }
  
  public EvalContext inScope(Scope scope) {
    return new EvalContext(scope, mThis);
  }
  
  public EvalContext innerScope() {
    return inScope(new Scope(mScope));
  }
  
  public Obj getThis() {
    return mThis;
  }
  
  public Obj lookUp(String name) {
    return mScope.get(name);
  }
  
  public void define(String name, Obj value) {
    mScope.define(name, value);
  }
  
  public void assign(String name, Obj value) {
    mScope.assign(name, value);
  }
  
  private final Scope mScope;
  private final Obj   mThis;
}
