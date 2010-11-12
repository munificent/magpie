package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public interface BuiltInCallable {
  Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
}
