package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Checker;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable that returns the value of a named field.
 */
public abstract class FieldProperty implements Callable {
  public FieldProperty(String name, Expr expr, boolean isInitializer) {
    Expect.notEmpty(name);
    Expect.notNull(expr);
    
    mName = name;
    mExpr = expr;
    mIsInitializer = isInitializer;
  }
  
  @Override
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj,
      List<Obj> typeArgs, Obj arg);

  public Obj getType(Interpreter interpreter) {
    if (mIsInitializer) {
      Checker checker = new Checker(interpreter);
      return checker.evaluateExpressionType(mExpr);
    } else {
      return interpreter.evaluate(mExpr);
    }
  }
  
  protected String getName() { return mName; }
  
  private final String mName;
  private final Expr mExpr;
  private final boolean mIsInitializer;
}
