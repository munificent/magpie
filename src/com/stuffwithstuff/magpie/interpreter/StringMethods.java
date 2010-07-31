package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class StringMethods {
  public static Invokable operatorPlus() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        String left = (String)thisObj.getPrimitiveValue();
        String right = (String)arg.getPrimitiveValue();
        
        return new Obj(thisObj.getClassObj(), left + right);
      }
    };
  }

  public static Invokable print() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        String string = (String)thisObj.getPrimitiveValue();
        
        interpreter.print(string);
        
        return interpreter.nothing();
      }
    };
  }
}
