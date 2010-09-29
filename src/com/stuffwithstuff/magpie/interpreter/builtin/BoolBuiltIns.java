package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BoolBuiltIns {
  @Signature("==(other Object -> Bool)")
  public static Obj _eqeq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    if (arg.getValue() instanceof Boolean) {
      return interpreter.createBool(thisObj.asBool() == arg.asBool());
    } else {
      return interpreter.createBool(false);
    }
  }

  @Signature("not(-> Bool)")
  public static Obj not(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.createBool(!thisObj.asBool());
  }
  
  @Signature("toString(-> String)")
  public static Obj toString(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.createString(Boolean.toString(thisObj.asBool()));
  }
}
