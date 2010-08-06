package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

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
  
  public Expr getParamType() { return mFunction.getParamType(); }
  public Expr getReturnType() { return mFunction.getReturnType(); }
  
  private final FnExpr mFunction;
}
