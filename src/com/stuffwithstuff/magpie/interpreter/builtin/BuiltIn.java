package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

public class BuiltIn implements Callable {
  public BuiltIn(FunctionType type, BuiltInCallable callable) {
    Expect.notNull(type);
    Expect.notNull(callable);
    
    mType = type;
    mCallable = callable;
  }

  @Override
  public FunctionType getType() {
    return mType;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    return mCallable.invoke(interpreter, thisObj, arg);
  }

  private final FunctionType    mType;
  private final BuiltInCallable mCallable;
}
