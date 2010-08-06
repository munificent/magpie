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
    
    EvalContext context = interpreter.createTopLevelContext();
    fn.getFunction().getBody().accept(checker, context);
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
      Obj declared = context.lookUpCheck(name);
      
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
    
    context.defineCheck(expr.getName(), value);
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
    Obj variable = context.lookUpCheck(expr.getName());
    if (variable != null) return variable;
    
    error(expr, "Variable \"%s\" must be previously defined.", expr.getName());

    // TODO(bob): Port from evaluator:
    /*
    Invokable method = context.getThis().findMethod(expr.getName());
    expect (method != null,
        "Could not find a variable named \"%s\".",
        expr.getName());
    return method.invoke(mInterpreter, context.getThis(), mInterpreter.nothing());
    */
    
    // Just return nothing and continue to find other errors.
    return mInterpreter.getNothingType();
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
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
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
