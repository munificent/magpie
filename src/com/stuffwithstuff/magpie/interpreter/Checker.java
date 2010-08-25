package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.parser.Position;

public class Checker {
  public Checker(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  public List<CheckError> checkAll() {
    // Check all of the reachable functions.
    for (Entry<String, Obj> entry : mInterpreter.getGlobals().entries()) {
      if (entry.getValue() instanceof FnObj) {
        FnObj function = (FnObj)entry.getValue();
        checkFunction(function.getFunction(), function.getClosure(),
            mInterpreter.getNothingType());
      } else if (entry.getValue() instanceof ClassObj) {
        ClassObj classObj = (ClassObj)entry.getValue();
        
        // Check all of the methods.
        for (Entry<String, Invokable> method : classObj.getMethods().entrySet()) {
          // Only check user-defined methods.
          if (method.getValue() instanceof FnObj) {
            FnObj function = (FnObj)method.getValue();
            checkFunction(function.getFunction(), function.getClosure(),
                classObj);
          }
        }
      }
    }
    
    return mErrors;
  }
  
  public Obj evaluateExpressionType(Expr expr) {
    ExprChecker checker = new ExprChecker(mInterpreter, this);

    Scope globals = typeScope(mInterpreter.getGlobals());
    EvalContext context = new EvalContext(globals, mInterpreter.getNothingType());

    // Get the expression's type.
    Obj type = checker.check(expr, context, true);
    
    // But if there are any type errors, don't return it.
    if (mErrors.size() > 0) return mInterpreter.nothing();
    
    return type;
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
  
  public Obj checkFunction(FnExpr function, Scope closure, Obj thisType) {
    // Evaluate the parameter type declaration expression to get the declared
    // parameter type(s).
    Obj paramType = evaluateType(function.getParamType());
    
    // Create a new local scope for the function.
    // TODO(bob): Walking the entire closure and getting its type could be
    // painfully slow here.
    Scope closureTypes = typeScope(closure);
    EvalContext functionContext = new EvalContext(
        closureTypes, thisType).nestScope();
    
    // Bind parameter types to their names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      functionContext.define(params.get(0), paramType);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the parameter is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        functionContext.define(params.get(i), paramType.getTupleField(i));
      }
    }
    
    ExprChecker checker = new ExprChecker(mInterpreter, this);
    Obj returnType = checker.checkFunction(function.getBody(), functionContext);

    // Check that the body returns a valid type.
    Obj expectedReturn = evaluateType(function.getReturnType());
    Obj matches = invokeMethod(expectedReturn, Identifiers.CAN_ASSIGN_FROM, returnType);
    
    if (!matches.asBool()) {
      String expectedText = invokeMethod(expectedReturn, Identifiers.TO_STRING).asString();
      String actualText = invokeMethod(returnType, Identifiers.TO_STRING).asString();
      addError(function.getReturnType().getPosition(),
          "Function is declared to return %s but is returning %s.",
          expectedText, actualText);
    }
    
    // TODO(bob): If this function is a method (i.e. this isn't nothing?), then we
    // also need to check that any assignment to a field matches the declared
    // type.
    
    // Always return the expected type so that we don't get cascading errors.
    return expectedReturn;
  }
  
  /**
   * Walks through a Scope containing values and invokes "type" on them,
   * yielding a new Scope where all of the names are bound to the types of the
   * values in the given Scope.
   */
  public Scope typeScope(Scope valueScope) {
    Scope scope = new Scope();
    for (Entry<String, Obj> entry : valueScope.entries()) {
      Obj type = mInterpreter.invokeMethod(entry.getValue(), Identifiers.TYPE,
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
