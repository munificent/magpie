package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

public class Checker {
  public Checker(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  public List<CheckError> check() {
    ExprChecker checker = new ExprChecker(this);
    
    // Check all of the reachable functions.
    EvalContext context = mInterpreter.createTopLevelContext();
    for (Entry<String, Obj> entry : mInterpreter.getGlobals().entries()) {
      if (entry.getValue() instanceof FnObj) {
//        checker.check((FnObj)entry.getValue(), context);
      }
    }

    // at load time, every object should know its type. in other words, for
    // every object, you should be able to send it a "type" message and get the
    // right object back.
    // for basic objects and instances of classes, its easy: just return the
    // class.
    // Tuple class should provide a "type" message that returns a tuple of the
    // types of its elements.
    // Array class should provide a "type" message that returns an ArrayType
    // object with the correct element type.
    // Function class should provide a "type" message that returns a
    // FunctionType with the correct return and param types.
    
    // this means that at check time, given a name, we can immediately tell its
    // type without having to (for example) step into function bodies.
    
    // What does it mean to type check (top down?)
    // - create a copy of the current global scope. this is because if we type
    //   check an assignment to a global var, we'll change its value, and we
    //   want to be able to restore it
    // - for each value in global scope
    //   if value is FnObj, type check fn
    //   if value is ClassObj, type check its methods

    // to type check a fn:
    // - dynamically evaluate its type signature to get the expected parameter
    //   types from the type annotations
    // - statically check the body expression and verify that the returned type
    //   matches the declared return type
    
    // to check an expression:
    // - message expr:
    //   - look up the receiver
    //   - check the arg
    //   - get its class
    //   - look up the method from the class
    //   - dynamically evaluate the method's type signature
    //   - make sure the param matches the arg
    //   - return the declared return type
    // - define expr:
    //   - check the value
    //   - create a TypedProxy and bind it to the name
    
    // a TypedProxy is a shadow of a real object. it's what the type checker
    // puts in place of an actual value when doing an assignment. it has a type
    // which is what the type checker cares about, but no actual state. thing of
    // it as a shell object whose only purpose is to respond correctly to a
    // "type" message
    return mErrors;
  }
  
  public Obj check(Expr expr, EvalContext context) {
    ExprChecker checker = new ExprChecker(this);
    return checker.check(expr, context);
  }
  
  public void error(Expr expr, String format, Object... args) {
    mErrors.add(new CheckError(
        expr.getPosition(), String.format(format, args)));
  }
  
  public Interpreter getInterpreter() { return mInterpreter; }
    
  private Interpreter mInterpreter;
  private final List<CheckError> mErrors = new ArrayList<CheckError>();
}
