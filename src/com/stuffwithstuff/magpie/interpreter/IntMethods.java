package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class IntMethods {
  public static Method operatorPlus() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        int left = ((Integer)thisObj.getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return new Obj(thisObj.getType(), left + right);
      }
    };
  }

  public static Method operatorMinus() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        int left = ((Integer)thisObj.getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return new Obj(thisObj.getType(), left + right);
      }
    };
  }
}
