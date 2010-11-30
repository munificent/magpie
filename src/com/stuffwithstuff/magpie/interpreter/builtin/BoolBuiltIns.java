package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BoolBuiltIns {
  @Signature("==(other Any -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      if (arg.getValue() instanceof Boolean) {
        return interpreter.createBool(thisObj.asBool() == arg.asBool());
      } else {
        return interpreter.createBool(false);
      }
    }
  }
  
  @Getter("not(-> Bool)")
  public static class Not implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
  }
  
  @Getter("string(-> String)")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
  }
}
