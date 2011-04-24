package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;

/**
 * Interface for an executable chunk of behavior. Implemented by Function for
 * user-defined functions and BuiltIn for built-in behavior.
 */
public interface Callable {
  Obj invoke(Interpreter interpreter, Obj arg);
  Pattern getPattern();
  Scope getClosure();
  String getDoc();
}
