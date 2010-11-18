package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * The type checker. Given an interpreter this walks through the entire global
 * scope and checks all of the reachable code. Type-checking mainly focuses on
 * three things:
 * 
 * 1. Are the arguments passed to a function compatible with the function's
 *    declared parameter types?
 * 2. Are the possible values returned by a function compatible with its
 *    declared return type?
 * 3. Are the methods invoked on an object allowed by its declared type?
 * 
 * There are some other minor things like reachability (not having code after a
 * "return"), but that's 95% of what the type-checker looks for.
 * 
 * The basic process is pretty simple. It walks through the global scope looking
 * for functions (or methods in classes). Then it walks the expression forming
 * the function's body, from the bottom leaf-most nodes (literals and names)
 * towards the root. It calculates the type of the expression on its way up and
 * tests that that type is valid there.
 */
public class Checker {
  public Checker(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  /**
   * Checks all of the reachable classes and functions defined in the global
   * scope. Returns a list of type-check errors found. If no errors were found,
   * it's safe to run main().
   */
  public void checkAll() {
    EvalContext staticContext = mInterpreter.createTopLevelContext();
    
    // Check all of the reachable functions.
    for (Entry<String, Obj> entry : mInterpreter.getGlobals().entries()) {
      if (entry.getValue() instanceof FnObj) {
        FnObj function = (FnObj)entry.getValue();
        checkFunction(function.getFunction(), mInterpreter.getNothingClass(),
            staticContext);
      } else if (entry.getValue() instanceof ClassObj) {
        checkClass((ClassObj)entry.getValue());
      }
    }
  }
  
  /**
   * Checks the given class and all of its members. Returns a list of errors
   * found.
   * 
   * @param   classObj  The class to check.
   */
  public void checkClass(ClassObj classObj) {
    EvalContext staticContext = mInterpreter.createTopLevelContext();
    
    // Check the constructor.
    DefiniteFieldAssignment assignment = new DefiniteFieldAssignment();
    assignment.check(this, classObj);
    
    // Check all of the methods.
    for (Entry<String, Callable> method : classObj.getMethods().entrySet()) {
      // Only check user-defined methods.
      if (method.getValue() instanceof Function) {
        Function function = (Function)method.getValue();
        checkFunction(function, classObj, staticContext);
      }
    }
    
    // Check all of the getters.
    for (Entry<String, Callable> getter : classObj.getGetters().entrySet()) {
      // Only check user-defined methods.
      if (getter.getValue() instanceof Function) {
        Function function = (Function)getter.getValue();
        checkFunction(function, classObj, staticContext);
        
        // Getter functions should not take any arguments.
        if (function.getType().getParamNames().size() > 0) {
          addError(function.getFunction().getPosition(),
              "The getter \"%s\" is declared to take one or more arguments, " +
              "but arguments are not allowed for a getter.",
              getter.getKey());
        }
      }
    }
  }
  
  /**
   * Type-checks the given function.
   * 
   * @param function  The function to type-check.
   * @param closure   The lexical scope where the function was defined.
   * @param thisType  The type of "this" within the function. In other words,
   *                  the class that this a method on.
   * @return          The evaluated type of the function.
   */
  public void checkFunction(Function function, Obj thisType,
      EvalContext staticContext) {
    // Nothing to check if it's built-in.
    if (function == null) return;
    
    checkFunction(function.getFunction(), function.getClosure(),
        thisType, staticContext);
  }
  
   /**
   * Type-checks the given function.
   * 
   * @param function  The function to type-check.
   * @param closure   The lexical scope where the function was defined.
   * @param thisType  The type of "this" within the function. In other words,
   *                  the class that this a method on.
   * @return          The evaluated type of the function.
   */
  public Obj checkFunction(FnExpr function, Scope closure, Obj thisType,
      EvalContext staticContext) {
    
    // Create a new local scope for the function.
    // TODO(bob): Walking the entire closure and getting its type could be
    // painfully slow here.
    Scope closureTypes = typeScope(closure);
    EvalContext functionContext = new EvalContext(
        closureTypes, thisType).pushScope();
    
    // Evaluate the function's type annotations in the static value scope.
    Obj paramType = mInterpreter.evaluate(function.getType().getParamType(),
        staticContext);

    // Bind the parameter names to their evaluated types.
    List<String> params = function.getType().getParamNames();
    functionContext.bind(mInterpreter, params, paramType);
    
    // If it's a static function, bind them in the static context too.
    if (function.isStatic()) {
      staticContext = staticContext.pushScope();
      staticContext.bind(mInterpreter, params, paramType);
    }

    // Evaluate the return type after binding the static parameters so that they
    // can in turn be used in the return type's signature, like foo[T -> T].
    Obj returnType = mInterpreter.evaluate(
        function.getType().getReturnType(), staticContext);

    // Check the body of the function.
    ExprChecker checker = new ExprChecker(mInterpreter, this, staticContext);
    Obj actualReturn = checker.checkFunction(function.getBody(),
        functionContext);
    
    // If it's declared to return Nothing, then we'll also allow (and ignore)
    // any other return type. Note that this doesn't mean we'll discard the
    // return value. Type annotations don't affect the behavior at all. It just
    // means that the checker will ignore any returned type if Nothing is
    // expected.
    checkTypes(returnType, actualReturn, true,
        function.getType().getReturnType().getPosition(),
        "Function is declared to return %s but is returning %s.");
    
    // Create the function type for the function.
    if (paramType == mInterpreter.nothing()) {
      throw new InterpreterException(String.format(
          "Could not evaluate parameter type %s.",
          function.getType().getParamType()));
    }
    if (returnType == mInterpreter.nothing()) {
      throw new InterpreterException(String.format(
          "Could not evaluate return type %s.",
          function.getType().getReturnType()));
    }
    
    return mInterpreter.invokeMethod(mInterpreter.getFunctionClass(),
        Identifiers.NEW_TYPE, mInterpreter.createTuple(
            paramType, returnType,
            mInterpreter.createBool(function.isStatic())));
  }
  
  /**
   * Gets the collection of errors that have been found by this checker so far.
   * It isn't guaranteed that all errors will be found in a single pass since
   * some errors may prevent others from being determined.
   * 
   * @return The errors found.
   */
  public List<CheckError> getErrors() { return mErrors; }
  
  /**
   * Evaluates the type of the given expression. Evaluates it in the context of
   * the global scope, so any local variables or other state where the
   * expression appears will not be known here.
   * 
   * @param   expr The expression to evaluate.
   * @return       The type of the expression.
   */
  public Obj evaluateExpressionType(Expr expr) {
    ExprChecker checker = new ExprChecker(mInterpreter, this, 
        mInterpreter.createTopLevelContext());

    Scope globals = typeScope(mInterpreter.getGlobals());
    EvalContext context = new EvalContext(globals,
        mInterpreter.getNothingClass());

    // Get the expression's type.
    Obj type = checker.check(expr, context, true);
    
    // But if there are any type errors, don't return it.
    if (mErrors.size() > 0) return mInterpreter.nothing();
    
    return type;
  }
  
  /**
   * Walks through a Scope containing values and invokes "type" on them,
   * yielding a new Scope where all of the names are bound to the types of the
   * values in the given Scope.
   * 
   * @param valueScope A scope containing named values.
   * @return           A scope with the same names mapped to the types of those
   *                   values.
   */
  // TODO(bob): Instead of doing this all at once, it's probably smarter to make
  // EvalContext and/or Scope support binding both values and types to a name
  // and then only translate values to types as needed.
  public Scope typeScope(Scope valueScope) {
    Scope parent = null;
    if (valueScope.getParent() != null) {
      parent = typeScope(valueScope.getParent());
    }

    Scope scope = new Scope(parent);
    for (Entry<String, Obj> entry : valueScope.entries()) {
      Obj type = mInterpreter.getMember(Position.none(), entry.getValue(),
          Identifiers.TYPE);
      scope.define(entry.getKey(), type);
    }
    
    return scope;
  }
  
  /**
   * Checks to see if a given type is compatible (i.e. can be assigned to) a
   * given expected type. If not, reports an error using the given message. The
   * message is expected to be a format string with two "%s" arguments: one for
   * the expected type and one for the actual type, in that order.
   * 
   * @param expected          The type expected at this point.
   * @param actual            The actual type found.
   * @param nothingOverrides  If true, then any actual type will be allowed if
   *                          the expected type is Nothing.
   * @param position          The position in the source where the type is being
   *                          checked.
   * @param message           A format string for the error message.
   */
  public void checkTypes(Obj expected, Obj actual,
      boolean nothingOverrides, Position position, String message) {
    Expect.notNull(expected);
    Expect.notNull(actual);
    
    boolean success;
    if (nothingOverrides && (expected == mInterpreter.getNothingClass())) {
      success = true;
    } else {
      Obj matches = mInterpreter.invokeMethod(expected,
          Identifiers.ASSIGNS_FROM, actual);
      success = matches.asBool();
    }
    
    if (!success) {
      String expectedText = mInterpreter.evaluateToString(expected);
      String actualText = mInterpreter.evaluateToString(actual);
      addError(position, message, expectedText, actualText);
    }
  }
  
  /**
   * Checks to see if a given type is compatible (i.e. can be assigned to) a
   * given expected type. If not, reports an error using the given message. The
   * message is expected to be a format string with two "%s" arguments: one for
   * the expected type and one for the actual type, in that order.
   * 
   * @param expected          The type expected at this point.
   * @param actual            The actual type found.
   * @param position          The position in the source where the type is being
   *                          checked.
   * @param message           A format string for the error message.
   */
  public void checkTypes(Obj expected, Obj actual, Position position, String message) {
    checkTypes(expected, actual, false, position, message);
  }

  /**
   * Adds the given error to the list of errors found by the type checker.
   * @param position  Where in the source code the error occurred.
   * @param format    A format string for the error message.
   * @param args      Values to be formatted by the format string.
   */
  public void addError(Position position, String format, Object... args) {
    mErrors.add(new CheckError(position, String.format(format, args)));
  }
    
  private Interpreter mInterpreter;
  private final List<CheckError> mErrors = new ArrayList<CheckError>();
}
