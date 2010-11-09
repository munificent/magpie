package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.FunctionType;

/**
 * Wraps a raw FnExpr in the data and logic needed to execute a user-defined
 * function given the context to do it in.
 */
public class Function implements Callable {
  public Function(Scope closure, FnExpr function) {
    mClosure = closure;
    mFunction = function;
  }

  public Scope getClosure() { return mClosure; }
  public FnExpr getFunction() { return mFunction; }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = new EvalContext(mClosure, thisObj).pushScope();
    
    // A missing argument is implied nothing.
    if (arg == null) arg = interpreter.nothing();
    
    // Bind arguments to their parameter names.
    if (arg == null) arg = interpreter.nothing();
    context.bind(interpreter, mFunction.getType().getParamNames(), arg);
    
    try {
      return interpreter.evaluate(mFunction.getBody(), context);
    } catch (ReturnException ex) {
      // There was an early return in the function, so return the value of that.
      return ex.getValue();
    }
  }
  
  @Override
  public FunctionType getType() { return mFunction.getType(); }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
