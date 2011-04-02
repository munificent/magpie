package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.NotImplementedException;

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
  public abstract Obj invoke(Interpreter interpreter, Obj arg);
  
  @Override
  public Pattern getPattern() {
    throw new NotImplementedException(
        "Field built-ins aren't ready for use in multimethods yet.");
  }
  
  protected String getName() { return mName; }
  
  private final String mName;
}
