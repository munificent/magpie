package com.stuffwithstuff.magpie.interpreter;

/**
 * Interface for an executable chunk of behavior. Implemented by Function for
 * user-defined functions and BuiltIn for built-in behavior.
 */
public interface Callable {
  Callable bindTo(ClassObj classObj);
  Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
}
