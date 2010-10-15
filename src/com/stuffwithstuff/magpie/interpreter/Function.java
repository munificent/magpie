package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

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
  
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = new EvalContext(mClosure, thisObj).pushScope();
    
    // A missing argument is implied nothing.
    if (arg == null) arg = interpreter.nothing();
    
    // Bind arguments to their parameter names.
    bindParameters(interpreter, context,
        mFunction.getType().getParamNames(), arg);
    
    try {
      return interpreter.evaluate(mFunction.getBody(), context);
    } catch (ReturnException ex) {
      // There was an early return in the function, so return the value of that.
      return ex.getValue();
    }
  }
  
  public FunctionType getType() { return mFunction.getType(); }
  
  // TODO(bob): There are a couple of places where we bind an object to names.
  // Should refactor to all use the same code.
  private void bindParameters(Interpreter interpreter, EvalContext context,
      List<String> names, Obj arg) {
    
    if (arg == null) arg = interpreter.nothing();
    
    if (names.size() == 1) {
      context.define(names.get(0), arg);
    } else if (names.size() > 1) {
      // Make sure the argument's structure matches our expected parameter list.
      // If it doesn't, ignore extra tuple fields and pad missing ones with
      // nothing.
      if (arg.getClassObj() != interpreter.getTupleType()) {
        // Not a tuple and we're expecting it to be, so just bind it to the
        // first parameter and define the others as nothing.
        context.define(names.get(0), arg);
        
        for (int i = 1; i < names.size(); i++) {
          context.define(names.get(1), interpreter.nothing());
        }
      } else {
        // Destructure the tuple.
        for (int i = 0; i < names.size(); i++) {
          Obj field = arg.getTupleField(i);
          if (field == null) field = interpreter.nothing();
          context.define(names.get(i), field);
        }
      }
    }
  }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
