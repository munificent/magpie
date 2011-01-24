package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

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
  public Obj invoke(Interpreter interpreter, Obj thisObj,
      List<Obj> typeArgs, Obj arg) {
    try {
      Profiler.push(mFunction.getPosition());
      
      // Create a local scope for the function.
      EvalContext context = new EvalContext(mClosure, thisObj).pushScope();
      
      // Bind the type arguments.
      List<Pair<String, Expr>> typeParams = mFunction.getType().getTypeParams();
      for (int i = 0; i < typeParams.size(); i++) {
        Obj typeArg;
        if ((typeArgs != null) && (i < typeArgs.size())) {
          typeArg = typeArgs.get(i);
        } else {
          typeArg = interpreter.nothing();
        }
        context.define(typeParams.get(i).getKey(), typeArg);
      }
      
      // Bind the arguments bounds to the pattern.
      Pattern pattern = mFunction.getType().getPattern();
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
  public Obj getType(Interpreter interpreter) {
    // Evaluate it in the context of its closure so that any static arguments
    // defined in the surrounding scope are available for use in this type
    // annotation.
    EvalContext context = new EvalContext(mClosure, interpreter.nothing());
   
    // TODO(bob): Should not do this if the function has type parameters
    // since we don't know what their values are.
    Obj type = interpreter.evaluateFunctionType(mFunction.getType(), context);

    // TODO(bob): Hackish. Cram the original function body in there too. That
    // way, if it's a static function, we have access to it during check time.
    type.setValue(mFunction);
    
    return type;
  }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
