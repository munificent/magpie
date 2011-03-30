package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Built-in callable that returns the value of a named field.
 */
public class FieldGetter extends FieldProperty implements Callable {
  public FieldGetter(String name) {
    super(name);
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    Obj value = thisObj.getField(getName());
    if (value == null) return interpreter.nothing();
    return value;
  }
}
