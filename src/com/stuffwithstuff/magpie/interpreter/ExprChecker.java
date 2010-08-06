package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

// TODO(bob): This code is almost identical to the actual ExprEvaluator. Should
// refactor and share code between them.

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  // TODO(bob): This is kinda temp.
  public static void check(Interpreter interpreter, List<CheckError> errors,
      FnObj fn) {
    ExprChecker checker = new ExprChecker(interpreter, errors);
    
    checker.invoke(interpreter.getNothingType(), fn.getFunction(),
        interpreter.getNothingType());
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
      // TODO(bob): Instead of just check ref equality, should eventually call
      // a canAssignTo? method on the actual "type" object itself.
      errorIf(declared != value, expr,
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
    int index = 0;
    for (Expr thisExpr : expr.getExpressions()) {
      result = check(thisExpr, localContext);
      
      // All but the last expression in a block should return nothing.
      // TODO(bob): We may want to relax this and discard their returns like
      // most statement-oriented languages allow.
      /*
      if (index < expr.getExpressions().size() - 1) {
        errorIf(result != mInterpreter.getNothingType(), thisExpr,
            "All but the last expression in a block must return nothing.");
      }*/
      index++;
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
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Make sure the conditions are bools.
    for (Expr condition : expr.getConditions()) {
      Obj conditionType = check(condition, context);
      errorIf(conditionType != mInterpreter.getBoolType(), condition,
          "Condition expression in an if expression must evaluate to Bool.");
    }
    
    // TODO(bob): Should relax this to return the union of the two arms.
    // Make sure the arms return the same thing.
    Obj thenArm = check(expr.getThen(), context);
    Obj elseArm = check(expr.getElse(), context);
    
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
      errorIf(conditionType != mInterpreter.getBoolType(), condition,
          "Condition expression in a while loop must evaluate to Bool.");
    }
    
    // Make sure the body returns nothing.
    Obj body = check(expr.getBody(), context);
    errorIf(body != mInterpreter.getNothingType(), expr.getBody(),
        "While loop body must return Nothing.");
    
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
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
    
    errorIf(paramType != mInterpreter.getNothingType(), expr,
        "Method \"%s\" on this expects a %s parameter and got %s.",
        expr.getName(), paramType, mInterpreter.getNothingType());
    
    return check(method.getReturnType(), methodTypeContext);
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
  
  private Obj invoke(Obj thisObj, FnExpr function, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = EvalContext.forMethod(mInterpreter.getGlobals(), thisObj);
    
    // Bind arguments to their parameter names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getTupleField(i));
      }
    }
    
    return check(function.getBody(), context);
  }

  private Invokable findMethodType(Obj typeObj, String name) {
    // TODO(bob): Only works with class objects right now. Eventually need a
    // more extensible generic way of doing this so we can handle (for example),
    // methods specific to an instance.
    if (!(typeObj instanceof ClassObj)) return null;
    
    ClassObj classObj = (ClassObj)typeObj;
    return classObj.findInstanceMethod(name);
  }
  
  private void errorIf(boolean condition, Expr expr,
      String format, Object... args) {
    if (condition) {
      error(expr, format, args);
    }
  }
  
  private void error(Expr expr, String format, Object... args) {
    mErrors.add(new CheckError(
        expr.getPosition(), String.format(format, args)));
  }
  
  private Obj check(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  private final List<CheckError> mErrors;
  private final Interpreter mInterpreter;
}
