package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Built-in callable that assigns a value to a named field.
 */
public class FieldSetter extends FieldProperty implements Callable {
  public FieldSetter(String name, Expr type, boolean isInitializer) {
    super(name, type, isInitializer);
  }
  
  public FieldSetter(String name, Expr type) {
    super(name, type, false);
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    thisObj.setField(getName(), arg);
    return arg;
  }
}
