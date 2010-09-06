package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FunctionBuiltIns {
  @Signature("type()")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)thisObj;
    
    return interpreter.evaluateFunctionType(function.getType());
  }
}
