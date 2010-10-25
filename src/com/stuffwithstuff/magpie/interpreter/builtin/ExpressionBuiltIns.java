package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ExpressionBuiltIns {
  @Getter("string(-> String)")
  public static Obj toString(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.createString("{ " + thisObj.getValue().toString() + " }");
  }
}
