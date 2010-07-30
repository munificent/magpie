package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class ClassMethods {
  public static Method addInstanceField() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        String name = (String)arg.getPrimitiveValue();
        thisClass.addInstanceField(name);
        
        return interpreter.nothing();
      }
    };
  }
}
