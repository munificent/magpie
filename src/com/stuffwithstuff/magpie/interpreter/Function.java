package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

/**
 * Wraps a raw FnExpr in the data and logic needed to execute a user-defined
 * function given the context to do it in.
 */
public class Function implements Callable {
  public Function(Scope closure, ClassObj containingClass, FnExpr function) {
    mClosure = closure;
    mContainingClass = containingClass;
    mFunction = function;
  }

  public Scope getClosure() { return mClosure; }
  public FnExpr getFunction() { return mFunction; }
  
  @Override
  public Callable bindTo(ClassObj classObj) {
    // When a user-defined function becomes a member of some class, it needs to
    // know that class so that when private members are accessed within its
    // body, they can be restricted to the appropriate class. Do that here.
    return new Function(mClosure, classObj, mFunction);
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    try {
      Profiler.push(mFunction.getPosition());
      
      // Create a local scope for the function.
      EvalContext context = new EvalContext(mClosure, mContainingClass).pushScope();
      
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
  private final ClassObj mContainingClass;
  private final FnExpr mFunction;
}
