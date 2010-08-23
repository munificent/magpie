package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj implements Invokable {
  public FnObj(ClassObj classObj, Scope closure, FnExpr function) {
    super(classObj);
    
    mClosure = closure;
    mFunction = function;
  }

  public FnExpr getFunction() { return mFunction; }
  
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = new EvalContext(mClosure, thisObj).nestScope();
    
    // Bind arguments to their parameter names.
    List<String> params = mFunction.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getTupleField(i));
      }
    }
    
    try {
      return interpreter.evaluate(mFunction.getBody(), context);
    } catch (ReturnException ex) {
      // There was an early return in the function, so return the value of that.
      return ex.getValue();
    }
  }
  
  public Obj check(Checker checker, EvalContext context) {
    // Evaluate the parameter type declaration expression to get the declared
    // parameter type(s).
    Obj paramType = checker.evaluateType(mFunction.getParamType());
    
    // Create a new local scope for the function.
    Scope closureTypes = checker.typeScope(mClosure);
    EvalContext functionContext = new EvalContext(
        closureTypes, context.getThis()).nestScope();
    
    // Bind parameter types to their names.
    List<String> params = mFunction.getParamNames();
    if (params.size() == 1) {
      functionContext.define(params.get(0), paramType);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the parameter is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        functionContext.define(params.get(i), paramType.getTupleField(i));
      }
    }
    
    Obj returnType = checker.checkFunction(mFunction.getBody(), functionContext);
    
    // Check that the body returns a valid type.
    Obj expectedReturn = checker.evaluateType(mFunction.getReturnType());
    Obj matches = checker.invokeMethod(expectedReturn, Identifiers.CAN_ASSIGN_FROM, returnType);
    
    if (!matches.asBool()) {
      String expectedText = checker.invokeMethod(expectedReturn, Identifiers.TO_STRING).asString();
      String actualText = checker.invokeMethod(returnType, Identifiers.TO_STRING).asString();
      checker.addError(getReturnType().getPosition(),
          "Function is declared to return %s but is returning %s.",
          expectedText, actualText);
    }
    
    // TODO(bob): If this FnObj is a method (i.e. this isn't nothing?), then we
    // also need to check that any assignment to a field matches the declared
    // type.
    
    // Always return the expected type so that we don't get cascading errors.
    return expectedReturn;
  }
  
  public Expr getParamType() { return mFunction.getParamType(); }
  public Expr getReturnType() { return mFunction.getReturnType(); }
  
  private final Scope mClosure;
  private final FnExpr mFunction;
}
