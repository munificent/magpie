package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.StaticFnExpr;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StaticFunctionBuiltIns {
  @Signature("type()")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    // TODO(bob): Hackish. We're just cramming the StaticFnExpr in a vanilla
    // Obj right now. Dirty!
    StaticFnExpr fn = (StaticFnExpr)thisObj.getValue();

    // TODO(bob): The static function object should probably have a closure
    // instead of using the top level context here.
    EvalContext context = interpreter.createTopLevelContext();
    return interpreter.evaluateStaticFunctionType(fn, context);
  }
}
