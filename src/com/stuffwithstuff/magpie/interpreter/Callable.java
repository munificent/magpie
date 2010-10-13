package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.FunctionType;

// TODO(bob): Get rid of thisObj arg since Magpie has bound methods?
/**
 * Interface for an object that can be called like a function. Implemented by
 * both FnObj (for normal user-defined functions) and NativMethod, to provide a
 * uniform way to invoke methods.
 */
public interface Callable {
  Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  FunctionType getType();
}
