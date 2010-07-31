package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on function objects.
 */
public class FnMethods {
  public static Invokable invoke() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
        FnObj thisFn = (FnObj)thisObj;
        
        return interpreter.invoke(thisFn.getParamNames(), thisFn.getBody(), arg);
      }
    };
  }
}
