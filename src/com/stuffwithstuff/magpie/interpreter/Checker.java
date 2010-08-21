package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.parser.Position;

public class Checker {
  public Checker(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  public List<CheckError> checkAll() {
    // Create a copy of the global scope and replace all defines values with
    // their types. From here on out, we'll be storing types in variables and
    // not values, and we need lookup to work with types uniformly up the scope
    // chain. This also lets us check expressions that assign to global
    // variables without trashing the actual value stored there.
    Scope globals = typeScope(mInterpreter.getGlobals());
    
    // Check all of the reachable functions.
    EvalContext context = new EvalContext(globals, mInterpreter.getNothingType());
    for (Entry<String, Obj> entry : mInterpreter.getGlobals().entries()) {
      if (entry.getValue() instanceof FnObj) {
        ((FnObj)entry.getValue()).check(this, context);
      } else if (entry.getValue() instanceof ClassObj) {
        ClassObj classObj = (ClassObj)entry.getValue();
        
        EvalContext classContext = context.withThis(classObj);
        
        // Check all of the methods.
        for (Entry<String, Invokable> method : classObj.getMethods().entrySet()) {
          // Only check user-defined methods.
          if (method.getValue() instanceof FnObj) {
            ((FnObj)method.getValue()).check(this, classContext);
          }
        }
      }
    }
    return mErrors;
  }
  
  public Obj evaluateType(Expr expr) {
    return mInterpreter.evaluateType(expr);
  }
  
  public Obj invokeMethod(Obj receiver, String name, Obj arg) {
    return mInterpreter.invokeMethod(receiver, name, arg);
  }
  
  public Obj invokeMethod(Obj receiver, String name) {
    return mInterpreter.invokeMethod(receiver, name, mInterpreter.nothing());
  }
  
  public Obj checkFunction(Expr expr, EvalContext context) {
    ExprChecker checker = new ExprChecker(mInterpreter, this);
    return checker.checkFunction(expr, context);
  }
  
  /**
   * Walks through a Scope containing values and invokes "type" on them,
   * yielding a new Scope where all of the names are bound to the types of the
   * values in the given Scope.
   */
  public Scope typeScope(Scope valueScope) {
    Scope scope = new Scope();
    for (Entry<String, Obj> entry : valueScope.entries()) {
      Obj type = mInterpreter.invokeMethod(entry.getValue(), "type",
          mInterpreter.nothing());
      scope.define(entry.getKey(), type);
    }
    
    return scope;
  }

  public void addError(Position position, String format, Object... args) {
    mErrors.add(new CheckError(position, String.format(format, args)));
  }
    
  private Interpreter mInterpreter;
  private final List<CheckError> mErrors = new ArrayList<CheckError>();
}
