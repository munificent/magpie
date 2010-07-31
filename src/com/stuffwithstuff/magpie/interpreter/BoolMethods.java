package com.stuffwithstuff.magpie.interpreter;

public class BoolMethods {
  public static Invokable toStringMethod() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        boolean value = ((Boolean)context.getThis().getPrimitiveValue()).booleanValue();
        return interpreter.createString(Boolean.toString(value));
      }
    };
  }
}
