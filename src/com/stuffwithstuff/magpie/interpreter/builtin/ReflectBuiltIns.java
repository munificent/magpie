package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ReflectBuiltIns {
  @Shared
  @Signature("same?(a, b -> Bool)")
  public static class Same implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Obj left = arg.getTupleField(0);
      Obj right = arg.getTupleField(1);
      
      return interpreter.createBool(left == right);
    }
  }

  @Shared
  @Signature("getClass(obj -> Class)")
  public static class GetClass implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return arg.getClassObj();
    }
  }
}
