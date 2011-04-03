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
  public FnObj(ClassObj classObj, Callable callable) {
    super(classObj);
    
    mCallable = callable;
  }

  public Callable getCallable() { return mCallable; }
  
  public Obj invoke(Interpreter interpreter, Obj arg) {
    return mCallable.invoke(interpreter, arg);
  }
  
  private final Callable mCallable;
}
