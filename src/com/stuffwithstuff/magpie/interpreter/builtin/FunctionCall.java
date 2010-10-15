package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable for handling a "call" message on a function object.
 */
public class FunctionCall implements Callable {
  public FunctionCall(FunctionType type) {
    Expect.notNull(type);
    
    mType = type;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)thisObj;
    
    return function.invoke(interpreter, arg);
  }
  
  public FunctionType getType() { return mType; }
  
  private final FunctionType mType;
}
