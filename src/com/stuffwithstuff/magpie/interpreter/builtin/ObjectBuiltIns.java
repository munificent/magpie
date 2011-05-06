package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ObjectBuiltIns {
  // TODO(bob): Rename toString.
  @Signature("(this) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj("<" + arg.getClassObj().getName() + ">");
    }
  }
}
