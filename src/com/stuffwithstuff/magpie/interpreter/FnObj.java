package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FunctionType;

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
    if (mCallable instanceof Function) {
      return (Function) mCallable;
    }
    
    return null;
  }
  
  public Obj invoke(Interpreter interpreter, Obj arg) {
    return mCallable.invoke(interpreter, mThis, arg);
  }
  
  public FunctionType getType() { return mCallable.getType(); }
  
  private final Obj mThis;
  private final Callable mCallable;
}
