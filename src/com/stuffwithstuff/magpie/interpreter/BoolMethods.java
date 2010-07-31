package com.stuffwithstuff.magpie.interpreter;

public class BoolMethods {
  public static Invokable toStringMethod() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        boolean value = ((Boolean)thisObj.getPrimitiveValue()).booleanValue();
        return interpreter.createString(Boolean.toString(value));
      }
    };
  }
}
