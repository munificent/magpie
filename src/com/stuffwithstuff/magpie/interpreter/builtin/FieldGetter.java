package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Built-in callable that returns the value of a named field.
 */
public class FieldGetter extends FieldProperty implements Callable {
  public FieldGetter(String name, Expr expr, boolean isInitializer) {
    super(name, expr, isInitializer);
  }
  
  public FieldGetter(String name, Expr expr) {
    super(name, expr, false);
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, 
      List<Obj> typeArgs, Obj arg) {
    Obj value = thisObj.getField(getName());
    if (value == null) return interpreter.nothing();
    return value;
  }
}
