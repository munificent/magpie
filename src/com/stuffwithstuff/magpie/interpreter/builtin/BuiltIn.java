package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BuiltIn implements Callable {
  public BuiltIn(Pattern pattern, BuiltInCallable callable) {
    mPattern = pattern;
    mCallable = callable;
  }

  @Override
  public Callable bindTo(ClassObj classObj) {
    // Since built-ins don't look up any members within their bodies, they
    // don't need to rebind.
    return this;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    return mCallable.invoke(interpreter, arg);
  }
  
  @Override
  public Pattern getPattern() { return mPattern; }

  private final Pattern mPattern;
  private final BuiltInCallable mCallable;
}
