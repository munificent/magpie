package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BuiltIn implements Callable {
  public BuiltIn(Pattern pattern, BuiltInCallable callable) {
    mPattern = pattern;
    mCallable = callable;
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
