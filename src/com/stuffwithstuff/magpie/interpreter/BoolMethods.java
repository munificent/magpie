package com.stuffwithstuff.magpie.interpreter;

public class BoolMethods {
  public static Method toStringMethod() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        boolean value = ((Boolean)thisObj.getPrimitiveValue()).booleanValue();
        return interpreter.createString(Boolean.toString(value));
      }
    };
  }
}
