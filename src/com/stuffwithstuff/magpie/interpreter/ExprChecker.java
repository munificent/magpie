package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

// TODO(bob): This code is almost identical to the actual ExprEvaluator. Should
// refactor and share code between them.

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  // TODO(bob): This is kinda temp.
  public static void check(Interpreter interpreter, List<CheckError> errors,
      Scope globalScope) {
    ExprChecker checker = new ExprChecker(interpreter, errors);
    
    // TODO(bob): Here, we need to go through the given globalScope and look up
    // the types of all of the values defined in it. Then we'll create a new
    // top-level scope that the type-checking functions here will use.
    // That way, when Foo is defined in the value global scope, we can have
    // access to its class in the checker's global scope.
    // (This gets a little tricky, though. We'll need to handle non-instance
    // methods at that point. If there is a class Foo, the type of the global
    // variable "Foo" is "Class", which doesn't have any of the specific
    // shared methods in Foo like "new".
    
    for (Entry<String, Obj> entry : globalScope.entries()) {
      // TODO(bob): Hack temp. Just check top-level functions for now.
      // Also need to check top-level classes.
      if (entry.getValue() instanceof FnObj) {
        FnExpr function = ((FnObj)entry.getValue()).getFunction();
        checker.check(function);
      }
    }
  }
  
  public ExprChecker(Interpreter interpreter, List<CheckError> errors) {
    mInterpreter = interpreter;
    mErrors = errors;
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    // TODO(bob): Implement commented out parts.
    
    if (expr.getTarget() == null) {
      // No target means we're just assigning to a variable (or field of this)
      // with the given name.
      String name = expr.getName();
      Obj value = check(expr.getValue(), context);
      
      // Try to assign to a local.
      Obj declared = context.lookUp(name);
      
      // Make sure the types match.
      expectType(value, declared, expr,
          "Cannot assign a %s value to a variable declared %s.",
          value, declared);
      
      // If not found, try to assign to a member of this.
      /*
      Invokable setter = context.getThis().findMethod(name + "=");
      if (setter != null) {
        return setter.invoke(mInterpreter, context.getThis(), value);
      }
      */
      
      return value;
    } else {
      /*
      // The target of the assignment is an actual expression, like a.b = c
      Obj target = evaluate(expr.getTarget(), context);
      Obj value = check(expr.getValue(), context);

      // If the assignment statement has an argument and a value, like:
      // a.b c = v (c is the arg, v is the value)
      // then bundle them together:
      if (expr.getTargetArg() != null) {
        Obj targetArg = evaluate(expr.getTargetArg(), context);
        value = mInterpreter.createTuple(context, targetArg, value);
      }

      // Look for a setter method.
      String setterName = expr.getName() + "=";
      Invokable setter = target.findMethod(setterName);
      
      expect(setter != null,
          "Could not find a method named \"%s\" on %s.", setterName, target);
      
      // Invoke the setter.
      return setter.invoke(mInterpreter, target, value);
      */
      return null;
    }
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    EvalContext localContext = context.newBlockScope();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = check(thisExpr, localContext);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.getBoolType();
  }

  @Override
  public Obj visit(CallExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(DefineExpr expr, EvalContext context) {
    Obj value = check(expr.getValue(), context);

    // Variables cannot be of type Nothing.
    errorIf(value == mInterpreter.getNothingType(), expr,
        "Cannot declare a variable \"%s\" of type Nothing.", expr.getName());
    
    context.define(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // fn (a Int, b String) print (a.toString + b)
    // create context
    // define params
    // check body
    // return obj representing fn type (i.e. an obj that knows param and return
    // type)
    
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Make sure the conditions are bools.
    for (Expr condition : expr.getConditions()) {
      Obj conditionType = check(condition, context);
      expectType(conditionType, mInterpreter.getBoolType(), condition,
          "Condition expression in an if expression must evaluate to Bool.");
    }
    
    // Make sure the arms return the same thing.
    Obj thenArm = check(expr.getThen(), context);
    Obj elseArm = check(expr.getElse(), context);
    
    // TODO(bob): Should relax this to return the union of the two arms.
    errorIf(thenArm != elseArm, expr.getThen(),
        "Both then and else arms of an if expression must return the same type.");
    
    return thenArm;
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    // Make sure the conditions are bools.
    for (Expr condition : expr.getConditions()) {
      Obj conditionType = check(condition, context);
      expectType(conditionType, mInterpreter.getBoolType(), condition,
          "Condition expression in a while loop must evaluate to Bool.");
    }
    
    // Make sure the body returns nothing.
    Obj body = check(expr.getBody(), context);
    expectType(body, mInterpreter.getNothingType(), expr.getBody(),
        "While loop body must return Nothing.");
    
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    Obj receiver = check(expr.getReceiver(), context);
    Obj arg = check(expr.getArg(), context);

    Invokable method = findMethodType(receiver, expr.getMethod());
    
    // TODO(bob): Copy/pasted from NameExpr    
    errorIf(method == null, expr,
        "Could not find a method named \"%s\" on type %s.",
        expr.getMethod(), receiver);

    // Just return an empty type and try to continute to find more errors.
    if (method == null) return mInterpreter.getNothingType();

    EvalContext methodTypeContext = mInterpreter.createTopLevelContext();
    Obj paramType = check(method.getParamType(), methodTypeContext);
    
    expectType(arg, paramType, expr,
        "Method \"%s\" on this expects a %s parameter and got %s.",
        expr.getMethod(), paramType, arg);
    
    return check(method.getReturnType(), methodTypeContext);
    // TODO(bob): Need to check against method's expected return type.
  }

  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // Look up a named variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    Invokable method = findMethodType(context.getThis(), expr.getName());
    
    // Make sure we could find a method.
    errorIf(method == null, expr,
        "Could not find a variable named \"%s\".",
        expr.getName());
    
    // Just return an empty type and try to continute to find more errors.
    if (method == null) return mInterpreter.getNothingType();
    
    EvalContext methodTypeContext = mInterpreter.createTopLevelContext();
    Obj paramType = check(method.getParamType(), methodTypeContext);
    
    expectType(mInterpreter.getNothingType(), paramType, expr,
        "Method \"%s\" on this expects a %s parameter and got %s.",
        expr.getName(), paramType, mInterpreter.getNothingType());
    
    return check(method.getReturnType(), methodTypeContext);
    // TODO(bob): Need to check against method's expected return type.
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.getStringType();
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    return context.getThis();
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // Evaluate the fields.
    Obj[] fields = new Obj[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = check(expr.getFields().get(i), context);
    }

    return mInterpreter.createTuple(context, fields);
  }
  
  /**
   * Checks the given function for type-safety. Virtually invokes it by binding
   * the parameters to their declared types and then checking the body of the
   * function. Returns the discovered return type of the function.
   * @return
   */
  private Obj check(FnExpr function) {
    // Create a new local scope for the function. This scope will be used to
    // hold *types* not values, which is why it doesn't root at the existing
    // global scope which holds the values defined at runtime.
    EvalContext context = EvalContext.topLevel(new Scope(),
        mInterpreter.getNothingType());
    
    // Bind parameter names to their declared types.
    Obj paramType = evaluateType(function.getParamType());

    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), paramType);
    } else if (params.size() > 1) {
      // The parser should ensure that the paramType object is a tuple with as
      // many fields as we have parameter names.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), paramType.getTupleField(i));
      }
    }
    
    Obj returnType = check(function.getBody(), context);
    Obj expectedReturn = evaluateType(function.getReturnType());
    expectType(returnType, expectedReturn, function,
        "Function is declared to return %s but is returning %s.",
        expectedReturn, returnType);
    
    return expectedReturn;
  }
  
  private boolean typeAllowed(Obj actual, Obj expected) {
    // Anything goes with dynamic.
    if (expected == mInterpreter.getDynamicType()) return true;
    
    // TODO(bob): Eventually a looser conversion process will happen here.
    return actual == expected;
  }
  
  private Obj check(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  private Obj evaluateType(Expr expr) {
    // Type expressions always evaluate in a clean context that only access
    // global scope. (For now at least. We may relax this later so that you
    // could, for example, reference classes declared locally in the same
    // scope.)
    EvalContext context = mInterpreter.createTopLevelContext();
    return mInterpreter.evaluate(expr, context);
  }

  private Invokable findMethodType(Obj typeObj, String name) {
    // TODO(bob): Only works with class objects right now. Eventually need a
    // more extensible generic way of doing this so we can handle (for example),
    // methods specific to an instance.
    if (!(typeObj instanceof ClassObj)) return null;
    
    ClassObj classObj = (ClassObj)typeObj;
    return classObj.findInstanceMethod(name);
    
    // TODO(bob): If typeObj is Dynamic, we should I think just return a
    // Dynamic -> Dynamic Invokable for any given name.
  }
  
  private void expectType(Obj actual, Obj expected, Expr expr, 
      String format, Object... args) {
    errorIf(!typeAllowed(actual, expected), expr, format, args);
  }
  
  private void errorIf(boolean condition, Expr expr,
      String format, Object... args) {
    if (condition) {
      error(expr, format, args);
    }
  }
  
  private void error(Expr expr, String format, Object... args) {
    String message = String.format(format, args);
    mErrors.add(new CheckError(expr.getPosition(), message));
  }
  
  private final List<CheckError> mErrors;
  private final Interpreter mInterpreter;
}
