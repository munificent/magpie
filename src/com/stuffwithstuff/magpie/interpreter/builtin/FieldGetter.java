package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

/**
 * Built-in callable that returns the value of a named field.
 */
public class FieldGetter implements Callable {
  public FieldGetter(ClassObj classObj, String name, Scope closure) {
    mName = name;
    mPattern = Pattern.type(Expr.name(classObj.getName()));
    mClosure = closure;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    Obj value = arg.getField(mName);
    if (value == null) return interpreter.nothing();
    return value;
  }
  
  @Override
  public Pattern getPattern() {
    return mPattern;
  }

  @Override
  public Scope getClosure() {
    return mClosure;
  }
  
  @Override
  public String getDoc() {
    // TODO(bob): Actual docs.
    return "<FieldGetter>";
  }

  private final String mName;
  private final Pattern mPattern;
  private final Scope mClosure;
}
