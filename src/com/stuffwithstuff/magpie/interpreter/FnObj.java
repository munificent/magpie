package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.type.FunctionType;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj implements Invokable {
  public FnObj(ClassObj classObj, FnExpr function) {
    super(classObj);
    
    mFunction = function;
  }

  public FnExpr getFunction() { return mFunction; }
  
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.invoke(thisObj, mFunction, arg);
  }
  
  public FunctionType getFunctionType() { return mFunction.getType(); }

  private final FnExpr mFunction;
}
