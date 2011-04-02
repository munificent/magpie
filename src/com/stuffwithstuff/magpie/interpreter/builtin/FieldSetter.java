package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Built-in callable that assigns a value to a named field.
 */
public class FieldSetter extends FieldProperty implements Callable {
  public FieldSetter(String name) {
    super(name);
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    arg.getTupleField(0).setField(getName(), arg.getTupleField(1));
    return arg;
  }
}
