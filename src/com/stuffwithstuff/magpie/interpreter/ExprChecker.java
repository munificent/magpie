package com.stuffwithstuff.magpie.interpreter;

import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

// TODO(bob): This code is almost identical to the actual ExprEvaluator. Should
// refactor and share code between them.

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  // TODO(bob): This is kinda temp.
  public static void check(Interpreter interpreter, List<Integer> errors,
      FnObj fn) {
    ExprChecker checker = new ExprChecker(interpreter, errors);
    
    EvalContext context = interpreter.createTopLevelContext();
    fn.getFunction().getBody().accept(checker, context);
  }
  
  public ExprChecker(Interpreter interpreter, List<Integer> errors) {
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
      Obj current = context.lookUpCheck(name);
      
      // Make sure the types match.
      // TODO(bob): Instead of just check ref equality, should eventually call
      // a canAssignTo? method on the actual "type" object itself.
      if (current != value) {
        mErrors.add(expr.getPosition().getStartLine());
      }
      
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
      // TODO(bob): Should check that non-last exprs are nothing.
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
    if (value == mInterpreter.getNothingType()) {
      mErrors.add(expr.getPosition().getStartLine());
    }
    
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
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
  }

  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    return null;
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
  
  private Obj check(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  private final List<Integer> mErrors;
  private final Interpreter mInterpreter;
}
