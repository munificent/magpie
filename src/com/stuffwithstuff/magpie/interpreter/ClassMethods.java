package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class ClassMethods {
  // TODO(bob): This is pretty much temp.
  public static Invokable newObj() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        // TODO(bob): Should do something with arg, initialize fields, etc.
        Obj proto = thisObj.getField("proto");
        return proto.spawn();
      }
    };
  }
}
