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
  public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
    return interpreter.invoke(context.getThis(), mFunction, arg);
  }

  private final FnExpr mFunction;
}
