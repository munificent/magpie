package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ObjectBuiltIns {
  // TODO(bob): Rename toString.
  @Signature("(_) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createString("<" + arg.getClassObj().getName() + ">");
    }
  }
}
