package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

// TODO(bob): Gross. Unify with Callable.
/**
 * Interface for an executable chunk of behavior. Implemented by Function for
 * user-defined functions and BuiltIn for built-in behavior.
 */
public interface TypeArgCallable extends Callable {
  Obj invoke(Interpreter interpreter, Obj thisObj, List<Obj> typeArgs, Obj arg);
}
