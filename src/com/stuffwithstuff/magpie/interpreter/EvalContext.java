package com.stuffwithstuff.magpie.interpreter;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope, this reference, etc.
 */
public class EvalContext {
  /**
   * Creates a new top-level EvalContext. This is the context in which a script
   * begins evaluating.
   * @param scope   The global scope.
   * @param nothing A reference to Nothing.
   */
  public static EvalContext topLevel(Scope scope, Obj nothing) {
    return new EvalContext(null, scope, nothing);
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   * Retains the same object, class and this but creates a fresh local scope.
   */
  public EvalContext newBlockScope() {
    return new EvalContext(this, new Scope(), mThis);
  }
  
  /**
   * Creates a new EvalContext for evaluating a method body. Will have a fresh
   * lexical scope and no object or class scope.
   */
  public static EvalContext forMethod(Scope closure, Obj thisObj) {
    EvalContext closureContext = new EvalContext(null, closure, thisObj);
    return closureContext.newBlockScope();
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference. Used to create the context that the body of a
   * method is evaluated within.
   */
  public EvalContext bindThis(Obj thisObj) {
    return new EvalContext(mParent, mScope, thisObj);
  }
  
  public Obj getThis() {
    return mThis;
  }
  
  public Obj lookUp(String name) {
    EvalContext context = this;
    while (context != null) {
      Obj value = context.mScope.get(name);
      if (value != null) return value;
      context = context.mParent;
    }
    
    return null;
  }
  
  public void define(String name, Obj value) {
    mScope.define(name, value);
  }
  
  public boolean assign(String name, Obj value) {
    EvalContext context = this;
    while (context != null) {
      if (context.mScope.assign(name, value)) return true;
      context = context.mParent;
    }
    
    return false;
  }
  
  private EvalContext(EvalContext parent, Scope scope, Obj thisObj) {
    mParent = parent;
    mScope = scope;
    mThis = thisObj;
  }

  private final EvalContext mParent;
  private final Scope mScope;
  private final Obj   mThis;
}
