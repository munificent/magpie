package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable that returns the value of a named field.
 */
public abstract class FieldProperty implements Callable {
  public FieldProperty(String name) {
    Expect.notEmpty(name);
    
    mName = name;
  }

  public Callable bindTo(ClassObj classObj) {
    return this;
  }

  @Override
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  protected String getName() { return mName; }
  
  private final String mName;
}
