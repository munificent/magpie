package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj {
  public FnObj(ClassObj classObj, Obj thisObj, Callable callable) {
    super(classObj);
    
    mThis = thisObj;
    mCallable = callable;
  }

  public Callable getCallable() { return mCallable; }
  
  public Function getFunction() {
    if (mCallable instanceof Function) return (Function) mCallable;
    
    return null;
  }
  
  public Obj invoke(Interpreter interpreter, List<Obj> typeArgs, Obj arg) {
    return mCallable.invoke(interpreter, mThis, typeArgs, arg);
  }
  
  private final Obj mThis;
  private final Callable mCallable;
}
