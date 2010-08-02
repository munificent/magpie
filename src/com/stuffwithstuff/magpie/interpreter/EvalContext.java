package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ScopeType;

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
    return new EvalContext(null, scope, null, null, nothing);
  }
  
  /**
   * Creates a new EvalContext within the given class.
   */
  public static EvalContext forClass(EvalContext context, Obj classObj) {
    return new EvalContext(context, new Scope(),
        classObj.getMember("proto").getScope(),
        classObj.getScope(), classObj);
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   * Retains the same object, class and this but creates a fresh local scope.
   */
  public EvalContext newBlockScope() {
    return new EvalContext(this, new Scope(), mObjectScope, mClassScope, mThis);
  }
  
  /**
   * Creates a new EvalContext for evaluating a method body. Will have a fresh
   * lexical scope and no object or class scope.
   */
  public static EvalContext forMethod(Scope closure, Obj thisObj) {
    EvalContext closureContext = new EvalContext(null, closure, null, null, thisObj);
    return closureContext.newBlockScope();
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference. Used to create the context that the body of a
   * method is evaluated within.
   */
  public EvalContext bindThis(Obj thisObj) {
    return new EvalContext(mParent, mScope, mObjectScope, mClassScope, thisObj);
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
    define(ScopeType.LOCAL, name, value);
  }
  
  public void define(ScopeType scope, String name, Obj value) {
    switch (scope) {
    case LOCAL:
      mScope.define(name, value);
      break;
      
    case OBJECT:
      if (mObjectScope == null) throw new InterpreterException("Cannot define an instance member outside of a class.");
      mObjectScope.define(name, value);
      break;
      
    case CLASS:
      if (mClassScope == null) throw new InterpreterException("Cannot define a shared member outside of a class.");
      mClassScope.define(name, value);
      break;
    }
  }
  
  public boolean assign(String name, Obj value) {
    EvalContext context = this;
    while (context != null) {
      if (context.mScope.assign(name, value)) return true;
      context = context.mParent;
    }
    
    return false;
  }
  
  private EvalContext(EvalContext parent, Scope localScope, Scope objectScope,
      Scope classScope, Obj thisObj) {
    mParent = parent;
    mScope = localScope;
    mObjectScope = objectScope;
    mClassScope = classScope;
    mThis = thisObj;
  }

  private final EvalContext mParent;
  private final Scope mScope;
  private final Scope mObjectScope;
  private final Scope mClassScope;
  private final Obj   mThis;
}
