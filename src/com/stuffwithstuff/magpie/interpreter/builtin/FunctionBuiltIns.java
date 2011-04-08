package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FunctionBuiltIns {
  @Signature("(_ Function) call(arg)")
  public static class Call implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      FnObj function = arg.getTupleField(0).asFn();
      return function.invoke(interpreter, arg.getTupleField(1));
    }
  }
}
