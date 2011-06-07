package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

public class IntrinsicCallable implements Callable {
  public IntrinsicCallable(Pattern pattern, String doc, Intrinsic callable, Scope closure) {
    mPattern = pattern;
    mDoc = doc;
    mCallable = callable;
    mClosure = closure;
  }

  @Override
  public Obj invoke(Context context, Obj arg) {
    return mCallable.invoke(context, arg.getField(0), arg.getField(1));
  }
  
  @Override
  public Pattern getPattern() { return mPattern; }

  @Override
  public Scope getClosure() {
    return mClosure;
  }
  
  @Override
  public String getDoc() {
    return mDoc;
  }
  
  private final Pattern mPattern;
  private final String mDoc;
  private final Intrinsic mCallable;
  private final Scope mClosure;
}
