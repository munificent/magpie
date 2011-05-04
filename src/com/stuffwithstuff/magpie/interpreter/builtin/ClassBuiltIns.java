package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ClassBuiltIns {
  @Signature("(this is Class) name")
  public static class Name implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      ClassObj classObj = arg.asClass();
      
      return interpreter.createString(classObj.getName());
    }
  }
}
