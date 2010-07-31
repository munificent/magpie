package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class StringMethods {
  public static Invokable operatorPlus() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        String left = (String)context.getThis().getPrimitiveValue();
        String right = (String)arg.getPrimitiveValue();
        
        return interpreter.createString(left + right);
      }
    };
  }

  public static Invokable print() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        String string = (String)context.getThis().getPrimitiveValue();
        
        interpreter.print(string);
        
        return interpreter.nothing();
      }
    };
  }
}
