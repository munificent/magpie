package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.CheckError;
import com.stuffwithstuff.magpie.interpreter.Checker;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.NullInterpreterHost;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class RuntimeBuiltIns {  
  // TODO(bob): All of these are pretty hacked together. Need to rationalize
  // the scope for these and clean them up.
  
  @Shared
  @Signature("now(-> Int)")
  public static Obj now(Interpreter interpreter, Obj thisObj, Obj arg) {
    // TODO(bob): Total hack to fit in an int.
    int time = (int)(System.currentTimeMillis() - 1289000000000L);
    return interpreter.createInt(time);
  }
  
  @Shared
  @Signature("throw(obj -> Never)")
  public static Obj throw_(Interpreter interpreter, Obj thisObj, Obj arg) {
    throw new ErrorException(arg);
  }
  
  @Shared
  // TODO(bob): Should be declared to return array of strings.
  @Signature("checkClass(classObj Class)")
  public static Obj checkClass(Interpreter interpreter, Obj thisObj, Obj arg) {
    Checker checker = new Checker(interpreter);
    
    checker.checkClass((ClassObj)arg);
    
    return translateErrors(interpreter, checker.getErrors());
  }

  @Shared
  // TODO(bob): Should be declared to return array of strings.
  @Signature("checkFunction(function)")
  public static Obj checkFunction(Interpreter interpreter, Obj thisObj, Obj arg) {
    Checker checker = new Checker(interpreter);
    
    FnObj function = (FnObj)arg;
    EvalContext staticContext = interpreter.createTopLevelContext();
    checker.checkFunction(function.getFunction(), interpreter.getNothingType(),
        staticContext);
    
    return translateErrors(interpreter, checker.getErrors());
  }

  @Shared
  // TODO(bob): Should be declared to return array of strings.
  @Signature("checkExpression(function)")
  public static Obj checkFunction2(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj function = (FnObj)arg;
    Expr expr = function.getFunction().getFunction().getBody();
    
    Interpreter inner = new Interpreter(new NullInterpreterHost());
    try {
      Script.loadBase(inner);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    // Pull out the list of expressions. If it's a BlockExpr, we do this so that
    // it doesn't evaluate the body in a nested scope.
    List<Expr> exprs;
    if (expr instanceof BlockExpr) {
      exprs = ((BlockExpr)expr).getExpressions();
    } else {
      exprs = new ArrayList<Expr>();
      exprs.add(expr);
    }
    
    inner.load(exprs);
  
    // Do the static analysis and see if we got the errors we expect.
    Checker checker = new Checker(inner);
    checker.checkAll();
    
    return translateErrors(interpreter, checker.getErrors());
  }

  private static Obj translateErrors(Interpreter interpreter, List<CheckError> errors) {
    List<Obj> errorObjs = new ArrayList<Obj>();
    for (CheckError error : errors) {
      // TODO(bob): Should eventually return more than just the error message.
      errorObjs.add(interpreter.createString(error.toString()));
    }

    return interpreter.createArray(errorObjs);
  }
}
