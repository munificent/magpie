package com.stuffwithstuff.magpie.interpreter;

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
  
  @Override
  public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
    return interpreter.invoke(context.getThis(), mFunction, arg);
  }

  private final FnExpr mFunction;
}
