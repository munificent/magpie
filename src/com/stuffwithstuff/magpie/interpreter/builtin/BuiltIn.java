package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

public class BuiltIn implements Callable {
  public BuiltIn(Pattern pattern, BuiltInCallable callable, Scope closure) {
    mPattern = pattern;
    mCallable = callable;
    mClosure = closure;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    return mCallable.invoke(interpreter, arg);
  }
  
  @Override
  public Pattern getPattern() { return mPattern; }

  @Override
  public Scope getClosure() {
    return mClosure;
  }
  
  private final Pattern mPattern;
  private final BuiltInCallable mCallable;
  private final Scope mClosure;
}
