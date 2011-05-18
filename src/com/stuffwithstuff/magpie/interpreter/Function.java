package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

/**
 * Wraps a raw FnExpr in the data and logic needed to execute a user-defined
 * function given the context to do it in.
 */
public class Function implements Callable {
  public Function(FnExpr function, Scope scope) {
    mFunction = function;
    mScope = scope;
  }

  @Override
  public Obj invoke(Context context, Obj arg) {
    try {
      Profiler.push(mFunction.getPosition());
      
      // Create a local scope for the function.
      Scope scope = mScope.push();
      
      // Bind the arguments to the pattern.
      Pattern pattern = mFunction.getPattern();
      PatternBinder.bind(context, false, pattern, arg, scope);
      
      try {
        return context.evaluate(mFunction.getBody(), scope);
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

  @Override
  public Scope getClosure() { return mScope; }

  @Override
  public String getDoc() { return mFunction.getDoc(); }
  
  private final FnExpr mFunction;
  private final Scope mScope;
}
