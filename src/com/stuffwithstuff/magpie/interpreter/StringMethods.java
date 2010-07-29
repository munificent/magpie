package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class StringMethods {
  public static Method operatorPlus() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        String left = (String)thisObj.getPrimitiveValue();
        String right = (String)arg.getPrimitiveValue();
        
        return new Obj(thisObj.getType(), left + right);
      }
    };
  }

  public static Method print() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        String string = (String)thisObj.getPrimitiveValue();
        
        interpreter.print(string);
        
        return interpreter.unit();
      }
    };
  }
}
