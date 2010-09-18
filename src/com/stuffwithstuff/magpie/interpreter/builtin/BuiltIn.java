package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

public class BuiltIn implements Callable {
  public BuiltIn(FunctionType type, Method method) {
    Expect.notNull(type);
    Expect.notNull(method);
    
    mType = type;
    mMethod = method;
  }
  
  @Override
  public FunctionType getType() {
    return mType;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    try {
      return (Obj)mMethod.invoke(null, interpreter, thisObj, arg);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    // Should not get here.
    return interpreter.nothing();
  }

  private final FunctionType mType;
  private final Method       mMethod;
}
