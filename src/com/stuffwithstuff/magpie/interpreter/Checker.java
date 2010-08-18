package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
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

    // to check an expression:
    // - message expr:
    //   - look up the receiver type
    //   - check the arg
    //   - look up the method from the receiver type
    //   - dynamically evaluate the method's type signature
    //   - make sure the param matches the arg
    //   - return the declared return type
    // - define expr:
    //   - check the value
    //   - bind the type to the name
    return mErrors;
  }
  
  public Obj evaluateType(Expr expr) {
    // We create a context from the interpreter here because we need to evaluate
    // type expressions in the regular interpreter context where scopes hold
    // values not types.
    EvalContext context = mInterpreter.createTopLevelContext();
    return mInterpreter.evaluate(expr, context);
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
