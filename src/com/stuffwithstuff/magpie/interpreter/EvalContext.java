package com.stuffwithstuff.magpie.interpreter;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope, this reference, etc.
 */
public class EvalContext {
  public static EvalContext forMethod(Scope outerScope, Obj thisObj) {
    EvalContext outerContext = new EvalContext(null, outerScope, thisObj);
    return outerContext.innerScope();
  }

  public EvalContext(EvalContext parent, Scope scope, Scope sharedScope,
      Obj thisObj) {
    mParent = parent;
    mScope = scope;
    mSharedScope = sharedScope;
    mThis = thisObj;
  }

  public EvalContext(EvalContext parent, Scope scope, Obj thisObj) {
    this(parent, scope, null, thisObj);
  }
  
  public EvalContext(Scope scope, Obj thisObj) {
    this(null, scope, thisObj);
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference. Used to create the context that the body of a
   * method is evaluated within.
   */
  public EvalContext bindThis(Obj thisObj) {
    return new EvalContext(mParent, mScope, thisObj);
  }
  
  /**
   * Creates a new EvalContext nested inside this one. It is used for block
   * scoping. It creates a context whose parent is this one, with its own Scope
   * and the same this reference.
   */
  public EvalContext innerScope() {
    return new EvalContext(this, new Scope(), mThis);
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
  
  public void defineShared(String name, Obj value) {
    if (mSharedScope == null) throw new InterpreterException("There is no shared scope here.");
    
    mSharedScope.define(name, value);
  }
  
  public boolean assign(String name, Obj value) {
    EvalContext context = this;
    while (context != null) {
      if (context.mScope.assign(name, value)) return true;
      context = context.mParent;
    }
    
    return false;
  }
  
  private final EvalContext mParent;
  private final Scope mScope;
  private final Scope mSharedScope;
  private final Obj   mThis;
}
