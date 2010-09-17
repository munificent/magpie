package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FunctionBuiltIns {
  @Signature("type()")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)thisObj;
    
    // Evaluate the function's type annotation in the context of its closure so
    // that the type can reference static type arguments in effect where the
    // function was defined.
    EvalContext context = new EvalContext(function.getClosure(),
        interpreter.nothing());
    return interpreter.evaluateFunctionType(function.getType(), context);
  }
}
