package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.type.FunctionType;

/**
 * Interface for something that can be invoked like a method: an entity that can
 * be called with a "this" reference and an argument.
 */
// TODO(bob): Is this needed anymore? Are only FnObjs invokable now?
public interface Invokable {
  Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  FunctionType getFunctionType();
}
