package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class ClassMethods {
  public static Invokable addInstanceField() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        String name = (String)arg.getPrimitiveValue();
        thisClass.getInstanceFields().put(name, true);
        
        return interpreter.nothing();
      }
    };
  }

  public static Invokable getName() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        return interpreter.createString(thisClass.getName());
      }
    };
  }

  public static Invokable instanceFieldQ() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        String name = (String)arg.getPrimitiveValue();
        return interpreter.createBool(thisClass.getInstanceFields().containsKey(name));
      }
    };
  }

  // TODO(bob): This is pretty much temp.
  public static Invokable construct() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        ClassObj thisClass = (ClassObj)thisObj;
        return new Obj(thisClass);
      }
    };
  }
}
