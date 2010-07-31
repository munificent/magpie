package com.stuffwithstuff.magpie.interpreter;

/**
 * Interface for something that can be invoked like a method: an entity that can
 * be called with a "this" reference and an argument.
 */
public interface Invokable {
  Obj invoke(Interpreter interpreter, EvalContext context, Obj arg);
}
