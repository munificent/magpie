package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FunctionType;

/**
 * Object type for a function object.
 */
public class BoundFnObj extends Obj implements Callable {
  public BoundFnObj(ClassObj classObj, Callable function, Obj thisObj) {
    super(classObj);
    
    mFunction = function;
    mThis = thisObj;
  }

  public Callable getFunction() { return mFunction; }
  public Obj getThis() { return mThis; }
  
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    return mFunction.invoke(interpreter, mThis, arg);
  }
  
  public FunctionType getType() { return mFunction.getType(); }
  
  private final Callable mFunction;
  private final Obj mThis;
}
