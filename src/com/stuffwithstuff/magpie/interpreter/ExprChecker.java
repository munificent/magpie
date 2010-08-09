package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

// TODO(bob): This code is almost identical to the actual ExprEvaluator. Should
// refactor and share code between them.

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  // TODO(bob): This is kinda temp.
  public static void check(Interpreter interpreter, List<CheckError> errors,
      Scope globalScope) {
    
    // Create a mirror of the interpreter's global scope where each global
    // variable has been replaced with its type. If, for example, there is a
    // global variable "foo" whose value is 123, the type scope will have a
    // global variable "foo" whose value is Int: the ClassObj representing the
    // class of Ints.
    Scope typeScope = new Scope();
    for (Entry<String, Obj> entry : globalScope.entries()) {
      typeScope.define(entry.getKey(), entry.getValue().getClassObj());
    }
    
    ExprChecker checker = new ExprChecker(interpreter, errors);
    
    // Create a new local scope for the function. This scope will be used to
    // hold *types* not values, unlike the regular expression evaluation
    // context.
    EvalContext context = new EvalContext(typeScope,
        interpreter.getNothingType());
    
    // Now check all of the reachable code.
    for (Entry<String, Obj> entry : globalScope.entries()) {
      // TODO(bob): Hack temp. Just check top-level functions for now.
      // Also need to check top-level classes.
      if (entry.getValue() instanceof FnObj) {
        checker.check((FnObj)entry.getValue(), context);
      } else if (entry.getValue() instanceof ClassObj) {
        checker.check((ClassObj)entry.getValue(), context);
      }
    }
  }
  
  private ExprChecker(Interpreter interpreter, List<CheckError> errors) {
    mInterpreter = interpreter;
    mErrors = errors;
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    if (expr.getTarget() == null) {
      // No target means we're just assigning to a variable (or field of this)
      // with the given name.
      String name = expr.getName();
      Obj value = check(expr.getValue(), context);
      
      // Try to assign to a local.
      Obj declared = context.lookUp(name);
      
      // If not found, try to assign to a member of this.
      if (declared == null) {
        FunctionType setter = findMethodType(context.getThis(), name + "=");
        if (setter != null) {
          declared = evaluateType(setter.getParamType());
        }
      }
      
      // Make sure the types match.
      expectType(value, declared, expr,
          "Cannot assign a %s value to a variable declared %s.",
          value, declared);
      
      return value;
    } else {
      // TODO(bob): Implement commented out parts.
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
      return mInterpreter.getDynamicType();
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
    return mInterpreter.getDynamicType();
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return mInterpreter.getDynamicType();
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
    // Check the body of the function.
    check(expr, context);
    
    // Return the type of the function itself.
    // TODO(bob): Implement me.
    System.out.println("Function types are not implemented.");
    return mInterpreter.getDynamicType();
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Make sure the conditions are bools.
    for (Condition condition : expr.getConditions()) {
      // TODO(bob): Need to handle let conditions here.
      Obj conditionType = check(condition.getBody(), context);
      expectType(conditionType, mInterpreter.getBoolType(), condition.getBody(),
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

    return checkMethod(expr, receiver, expr.getMethod(), arg);
  }
  
  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // Look up a named variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    // Look for a method on this: foo -> this.foo ()
    return checkMethod(expr, context.getThis(), expr.getName(), mInterpreter.getNothingType());
  }
  
  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    System.out.println("Return expressions are not implemented.");
    return mInterpreter.getDynamicType();
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
  
  private Obj check(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }

  /**
   * Checks the given function for type-safety. Virtually invokes it by binding
   * the parameters to their declared types and then checking the body of the
   * function. Returns the discovered return type of the function.
   * @return
   */
  private Obj check(FnObj obj, EvalContext context) {
    return check(obj.getFunction(), context);
  }
  
  private Obj check(FnExpr function, EvalContext context) {
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

  private void check(ClassObj classObj, EvalContext context) {
    // Create a context for the methods that binds the class as the type of
    // this.
    EvalContext classContext = context.bindThis(classObj).newBlockScope();

    // We've got to do something a little fishy here. For declared fields like
    // "foo Int", the getter and setter methods for the class already have a
    // type expression defined that we can check against.
    // For defined fields, like "foo = 123", we don't. The type must be inferred
    // from evaluating that expression. We'll do that evaluation here and store
    // the result in local variables that shadow the "real" getters and setters.
    Map<String, Expr> fields = classObj.getFieldInitializers();
    if (fields != null) {
      for (Entry<String, Expr> entry : classObj.getFieldInitializers().entrySet()) {
        // Note: we use the original context here not the class one because field
        // initializers don't have access to anything defined within the class.
        Obj fieldType = check(entry.getValue(), context);
        classContext.define(entry.getKey(), fieldType);
      }
    }
    
    // Check the methods.
    for (Entry<String, Invokable> entry : classObj.getMethods().entrySet()) {
      // Ignore native methods.
      if (entry.getValue() instanceof FnObj) {
        check((FnObj)entry.getValue(), classContext);
      }
    }
  }
  
  private Obj checkMethod(Expr expr, Obj receiver, String name, Obj arg) {
    FunctionType method = findMethodType(receiver, name);
    
    // Make sure we could find a method.
    errorIf(method == null, expr,
        "Could not find a variable or method named \"%s\".", name);

    // Just return an empty type and try to continute to find more errors.
    if (method == null) return mInterpreter.getNothingType();
    
    EvalContext methodTypeContext = mInterpreter.createTopLevelContext();
    Obj paramType = check(method.getParamType(), methodTypeContext);
    
    expectType(arg, paramType, expr,
        "Method \"%s\" expects %s and got %s.",
        name, paramType, arg);
    
    return check(method.getReturnType(), methodTypeContext);
    // TODO(bob): Need to check against method's expected return type.
  }
  
  private boolean typeAllowed(Obj actual, Obj expected) {
    // Anything goes with dynamic.
    if (expected == mInterpreter.getDynamicType()) return true;
    
    // TODO(bob): Eventually a looser conversion process will happen here.
    return actual == expected;
  }
  
  private Obj evaluateType(Expr expr) {
    // Type expressions always evaluate in a clean context that only access
    // global scope. (For now at least. We may relax this later so that you
    // could, for example, reference classes declared locally in the same
    // scope.)
    EvalContext context = mInterpreter.createTopLevelContext();
    return mInterpreter.evaluate(expr, context);
  }

  private FunctionType findMethodType(Obj typeObj, String name) {
    // If the class is "Dynamic" all methods are presumed to exist and be typed
    // Dynamic -> Dynamic.
    if (typeObj == mInterpreter.getDynamicType()) {
      return new FunctionType(new NameExpr("Dynamic"), new NameExpr("Dynamic"));
    }
    
    ClassObj classObj = (ClassObj)typeObj;
    Invokable method = classObj.findMethod(name);
    
    if (method != null) {
      // TODO(bob): Invokable should really just return the FunctionType
      // directly.
      return new FunctionType(method.getParamType(), method.getReturnType());
    }
    
    return null;
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
