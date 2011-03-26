package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class MultimethodBuiltIns {
  @Getter("type Type")
  public static class Type implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // TODO(bob): Hack temp. We just need some object that has the original
      // multimethod in it so the checker can pull it back out.
      return interpreter.instantiate((ClassObj)interpreter.getGlobal("MultimethodType"), thisObj);
    }
  }
}
