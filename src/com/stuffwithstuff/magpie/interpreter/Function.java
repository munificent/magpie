package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

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
  public Obj invoke(Interpreter interpreter, Obj arg) {
    try {
      Profiler.push(mFunction.getPosition());
      
      // Create a local scope for the function.
      EvalContext context = new EvalContext(mClosure).pushScope();
      
      // Bind the arguments bounds to the pattern.
      Pattern pattern = mFunction.getPattern();
      PatternBinder.bind(interpreter, pattern, arg, context);
      
      try {
        return interpreter.evaluate(mFunction.getBody(), context);
      } catch (ReturnException ex) {
        // There was an early return in the function, so return the value of that.
        return ex.getValue();
      }
    } finally {
      Profiler.pop();
    }
  }
  
  @Override
  public Pattern getPattern() { return mFunction.getPattern(); }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
