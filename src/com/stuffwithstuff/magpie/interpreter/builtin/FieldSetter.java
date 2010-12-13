package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.Collections;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable that assigns a value to a named field.
 */
public class FieldSetter implements Callable {
  public FieldSetter(String name, Expr type) {
    Expect.notEmpty(name);
    Expect.notNull(type);
    
    mName = name;
    mType = type;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    thisObj.setField(mName, arg);
    return arg;
  }
  
  public Obj getType(Interpreter interpreter)
  {
    FunctionType type = new FunctionType(Collections.singletonList("value"), mType, mType,
        false);
    
    return interpreter.evaluateFunctionType(type, null);
  }

  private final String mName;
  private final Expr mType;
}
