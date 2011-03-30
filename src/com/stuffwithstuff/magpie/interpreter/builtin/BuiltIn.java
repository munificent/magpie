package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BuiltIn implements Callable {
  public BuiltIn(BuiltInCallable callable) {
    mCallable = callable;
  }

  @Override
  public Callable bindTo(ClassObj classObj) {
    // Since built-ins don't look up any members within their bodies, they
    // don't need to rebind.
    return this;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    return mCallable.invoke(interpreter, thisObj, arg);
  }

  private final BuiltInCallable mCallable;
}
