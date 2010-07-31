package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * An implementation of Invokable for a regular user-defined method.
 */
public class Method implements Invokable {
  public Method(FnExpr function) {
    mFunction = function;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.invoke(mFunction.getParamNames(), mFunction.getBody(), arg);
  }

  private final FnExpr mFunction;
}
