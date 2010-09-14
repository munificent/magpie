package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.Collections;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable that returns the value of a named field.
 */
public class FieldGetter implements Callable {
  public FieldGetter(String name, Expr type) {
    Expect.notEmpty(name);
    Expect.notNull(type);
    
    mName = name;
    mType = type;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj staticArg, Obj arg) {
    Obj value = thisObj.getField(mName);
    if (value == null) return interpreter.nothing();
    return value;
  }
  
  public FunctionType getType() {
    return new FunctionType(Collections.<String>emptyList(),
        Expr.name("Nothing"), mType);
  }

  private final String mName;
  private final Expr mType;
}
