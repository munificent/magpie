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

  public static Method name() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        return interpreter.createString(thisClass.getName());
      }
    };
  }

  public static Method instanceFieldQ() {
    return new Method() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        String name = (String)arg.getPrimitiveValue();
        return interpreter.createBool(thisClass.getInstanceFields().containsKey(name));
      }
    };
  }
}
