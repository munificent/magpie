package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

/**
 * Built-in callable for creating an instance of some class. It relies on
 * "init()" to do the actual initialization. "new()" just creates a fresh
 * object and stores it where the "init()" built-in can find it.
 */
public class ClassNew implements Callable {
  public ClassNew(Scope closure) {
    mClosure = closure;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    // Get the class being constructed.
    ClassObj classObj = arg.getTupleField(0).asClass();
    return interpreter.constructNewObject(classObj, arg.getTupleField(1));
  }
  
  @Override
  public Pattern getPattern() {
    // The receiver is any instance of Class, and it takes any argument, since
    // it will simply forward it onto 'init()'.
    return Pattern.tuple(
        Pattern.type(Expr.variable("Class")),
        Pattern.wildcard());
  }

  @Override
  public Scope getClosure() {
    return mClosure;
  }

  private final Scope mClosure;
}
