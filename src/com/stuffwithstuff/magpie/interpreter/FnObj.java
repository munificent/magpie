package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj implements Invokable {
  public FnObj(ClassObj classObj, Scope closure, FnExpr function) {
    super(classObj);
    
    mClosure = closure;
    mFunction = function;
  }

  public Scope getClosure() { return mClosure; }
  public FnExpr getFunction() { return mFunction; }
  
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = new EvalContext(mClosure, thisObj).nestScope();
    
    // Bind arguments to their parameter names.
    List<String> params = mFunction.getType().getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // Make sure the argument's structure matches our expected parameter list.
      // If it doesn't, ignore extra tuple fields and pad missing ones with
      // nothing.
      if (arg.getClassObj() != interpreter.getTupleType()) {
        // Not a tuple and we're expecting it to be, so just bind it to the
        // first parameter and define the others as nothing.
        context.define(params.get(0), arg);
        
        for (int i = 1; i < params.size(); i++) {
          context.define(params.get(1), interpreter.nothing());
        }
      } else {
        // Destructure the tuple.
        for (int i = 0; i < params.size(); i++) {
          Obj field = arg.getTupleField(i);
          if (field == null) field = interpreter.nothing();
          context.define(params.get(i), field);
        }
      }
    }
    
    try {
      return interpreter.evaluate(mFunction.getBody(), context);
    } catch (ReturnException ex) {
      // There was an early return in the function, so return the value of that.
      return ex.getValue();
    }
  }
  
  public Expr getParamType() { return mFunction.getType().getParamType(); }
  public Expr getReturnType() { return mFunction.getType().getReturnType(); }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
