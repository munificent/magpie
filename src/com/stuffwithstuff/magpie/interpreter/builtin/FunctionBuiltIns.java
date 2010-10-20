package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FunctionBuiltIns {
  @Getter("type(-> Class)")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)thisObj;
    
    return interpreter.evaluateCallableType(function.getCallable(), false);
  }
}
