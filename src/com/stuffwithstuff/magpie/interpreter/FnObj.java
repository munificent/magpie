package com.stuffwithstuff.magpie.interpreter;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj {
  /**
   * Creates a new FnObj.
   * 
   * @param classObj     The class of the function object itself: Function.
   * @param thisObj      The object that "this" is bound to in the body of the
   *                     function. For functions that are created by looking up
   *                     a method, this will be the receiver.
   * @param callable
   */
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
  
  public Obj invoke(Interpreter interpreter, Obj arg) {
    return mCallable.invoke(interpreter, mThis, arg);
  }
  
  private final Obj mThis;
  private final Callable mCallable;
}
