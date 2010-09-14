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
// TODO(bob): Get rid of this and make a single built-in unsafeCast method that
// takes a static argument.
public class UnsafeCast implements Callable {
  public UnsafeCast(String className) {
    Expect.notEmpty(className);
    
    mClassName = className;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj staticArg, Obj arg) {
    if (arg.getClassObj() != thisObj) {
      interpreter.runtimeError(
          "Cannot assign %s as the base class for %s because it is not a class.",
          arg, thisObj);
      
      // TODO(bob): Should throw an exception. Returning nothing here violates
      // the return type.
      return interpreter.nothing();
    }
    
    // Just echo the argument back. The important part is tha the annotated
    // type has changed.
    return arg;
  }

  public FunctionType getType() {
    return new FunctionType(Collections.singletonList("object"),
        Expr.name("Dynamic"), Expr.name(mClassName));
  }
  
  private final String mClassName;
}
