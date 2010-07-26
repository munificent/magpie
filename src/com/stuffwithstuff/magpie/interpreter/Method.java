package com.stuffwithstuff.magpie.interpreter;

/**
 * Interface for a method. A method is some entity that can be called with a
 * "this" reference and an argument.
 */
public interface Method {
  Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
}
