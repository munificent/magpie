package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

public class BuiltIn implements Callable {
  public BuiltIn(Pattern pattern, BuiltInCallable callable, Scope closure) {
    mPattern = pattern;
    mCallable = callable;
    mClosure = closure;
  }

  @Override
  public Obj invoke(Context context, Obj arg) {
    return mCallable.invoke(context, arg);
  }
  
  @Override
  public Pattern getPattern() { return mPattern; }

  @Override
  public Scope getClosure() {
    return mClosure;
  }
  
  @Override
  public String getDoc() {
    // TODO(bob): Define Java annotation to let built-ins provide this.
    return "<built-in>";
  }
  
  private final Pattern mPattern;
  private final BuiltInCallable mCallable;
  private final Scope mClosure;
}
