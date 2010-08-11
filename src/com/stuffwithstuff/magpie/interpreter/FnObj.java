package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

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
    // Create a new local scope for the function.
    EvalContext context = EvalContext.forMethod(
        interpreter.getGlobals(), thisObj);
    
    // Bind arguments to their parameter names.
    List<String> params = mFunction.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getTupleField(i));
      }
    }
    
    try {
      return interpreter.evaluate(mFunction.getBody(), context);
    } catch (ReturnException ex) {
      // There was an early return in the function, so return the value of that.
      return ex.getValue();
    }
  }
  
  public Expr getParamType() { return mFunction.getParamType(); }
  public Expr getReturnType() { return mFunction.getReturnType(); }
  
  private final FnExpr mFunction;
}
