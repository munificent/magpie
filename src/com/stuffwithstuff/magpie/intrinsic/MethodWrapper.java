package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Method;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.InterpreterException;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class MethodWrapper implements Intrinsic {
  public MethodWrapper(Method method) {
    mMethod = method;
  }
  
  @Override
  public Obj invoke(Context context, Obj left, Obj right) {
    Object result = mMethod.call(left.getValue(), right.getValue());
    
    if (result == null) {
      return context.nothing();
    } else if (result instanceof Boolean) {
      return context.toObj((Boolean)result);
    } else if (result instanceof Integer) {
      return context.toObj((Integer)result);
    } else if (result instanceof String) {
      return context.toObj((String)result);
    } else {
      // TODO(bob): Better error.
      throw new InterpreterException("???");
    }
  }
  
  private final Method mMethod;
}
