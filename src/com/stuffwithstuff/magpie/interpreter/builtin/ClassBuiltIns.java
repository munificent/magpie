package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ClassBuiltIns {
  @Signature("(this is Class) name")
  public static class Name implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj(arg.asClass().getName());
    }
  }
}
