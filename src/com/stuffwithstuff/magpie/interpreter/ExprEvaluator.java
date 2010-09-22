package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;

/**
 * Implements the visitor pattern on AST nodes, in order to evaluate
 * expressions. This is the heart of the interpreter and is where Magpie code is
 * actually executed.
 */
public class ExprEvaluator implements ExprVisitor<Obj, EvalContext> {
  public ExprEvaluator(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  /**
   * Evaluates the given expression in the given context.
   * @param   expr     The expression to evaluate.
   * @param   context  The context in which to evaluate the expression.
   * @return           The result of evaluating the expression.
   */
  public Obj evaluate(Expr expr, EvalContext context) {
    if (expr == null) return null;
    return expr.accept(this, context);
  }

  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
        
    if (isTruthy(expr, left)) {
      return evaluate(expr.getRight(), context);
    } else {
      return left;
    }
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    String name = expr.getName();
    
    // Try to assign to a local.
    Obj value = evaluate(expr.getValue(), context);    
    if (context.assign(name, value)) return value;
    
    // Otherwise, it must be a setter on this.
    String setter = Identifiers.makeSetter(name);
    return mInterpreter.invokeMethod(expr, context.getThis(), setter, value);
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    if (expr.createScope()) {
      context = context.pushScope();
    }
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, context);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.createBool(expr.getValue());
  }

  @Override
  public Obj visit(BreakExpr expr, EvalContext context) {
    throw new BreakException();
  }

  @Override
  public Obj visit(ExpressionExpr expr, EvalContext context) {
    return mInterpreter.getExpressionType().instantiate(expr.getBody());
  }
  
  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return mInterpreter.createFn(expr, context.getScope());
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Put it in a block so that variables declared in conditions end when the
    // if expression ends.
    context = context.pushScope();
    
    // Evaluate all of the conditions.
    boolean passed = true;
    for (Condition condition : expr.getConditions()) {
      if (condition.isLet()) {
        // "let" condition.
        Obj result = evaluate(condition.getBody(), context);
        
        // If it evaluates to nothing, the condition fails. Otherwise, bind the
        // result to a name and continue.
        if (result != mInterpreter.nothing()) {
          // Success, bind the result.
          context.define(condition.getName(), result);
        } else {
          // Condition failed.
          passed = false;
          break;
        }
      } else {
        // Regular "if" condition.
        Obj result = evaluate(condition.getBody(), context);
        if (!isTruthy(expr, result)) {
          // Condition failed.
          passed = false;
          break;
        }
      }
    }
    
    // Evaluate the body.
    if (passed) {
      return evaluate(expr.getThen(), context);
    } else {
      return evaluate(expr.getElse(), context);
    }
  }

  @Override
  public Obj visit(InstantiateExpr expr, EvalContext context) {
    Obj fn = evaluate(expr.getFn(), context);
    Obj arg = evaluate(expr.getArg(), context);
    
    // TODO(bob): Unchecked cast = lame!
    StaticFnExpr staticFn = (StaticFnExpr)fn.getValue();
    
    // Bind the argument(s) to the static parameter(s).
    context = context.pushScope();
    if (staticFn.getParams().size() > 1) {
      // TODO(bob): Gross, assume arg is a tuple.
      for (int i = 0; i < staticFn.getParams().size(); i++) {
        context.define(staticFn.getParams().get(i), arg.getTupleField(i));
      }
    } else if (staticFn.getParams().size() == 1) {
      context.define(staticFn.getParams().get(0), arg);
    }
    
    // Now evaluate the body in that context.
    return evaluate(staticFn.getBody(), context);
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    try {
      boolean done = false;
      while (true) {
        // Evaluate the conditions.
        for (Expr conditionExpr : expr.getConditions()) {
          // See if the while clause is still true.
          Obj condition = evaluate(conditionExpr, context);
          if (!isTruthy(conditionExpr, condition)) {
            done = true;
            break;
          }
        }
        
        // If any clause failed, stop the loop.
        if (done) break;
        
        evaluate(expr.getBody(), context);
      }
    } catch (BreakException ex) {
      // Nothing to do.
    }
    
    // TODO(bob): It would be cool if loops could have "else" clauses and then
    // reliably return a value.
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    Obj receiver = evaluate(expr.getReceiver(), context);
    Obj arg = evaluate(expr.getArg(), context);
    
    if (receiver == null) {
      // Just a name, so maybe it's a variable.
      Obj variable = context.lookUp(expr.getName());

      if (variable != null) {
        // If we have an argument, apply it.
        if (arg != null) {
          return mInterpreter.invokeMethod(
              expr, variable, Identifiers.CALL, arg);
        }
        return variable;
      }
      
      // Otherwise it must be a method on this.
      return mInterpreter.invokeMethod(
          expr, context.getThis(), expr.getName(), arg);
    }
    
    return mInterpreter.invokeMethod(expr, receiver, expr.getName(), arg);
  }
  
  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
    
    if (isTruthy(expr, left)) {
      return left;
    } else {
      return evaluate(expr.getRight(), context);
    }
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);
    throw new ReturnException(value);
  }

  @Override
  public Obj visit(StaticFnExpr expr, EvalContext context) {
    // TODO(bob): Ghetto! Should have a real class and probably close over the
    // current context.
    return new Obj(mInterpreter.getStaticFunctionType(), expr);
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.createString(expr.getValue());
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    return context.getThis();
  }

  @Override
  public Obj visit(TypeofExpr expr, EvalContext context) {
    Checker checker = new Checker(mInterpreter);
    return checker.evaluateExpressionType(expr.getBody());
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // Evaluate the fields.
    Obj[] fields = new Obj[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = evaluate(expr.getFields().get(i), context);
    }

    return mInterpreter.createTuple(fields);
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    context.define(expr.getName(), value);
    return value;
  }
  
  private boolean isTruthy(Expr expr, Obj receiver) {
    Obj truthy = mInterpreter.invokeMethod(receiver, Identifiers.IS_TRUE);
    return truthy.asBool();
  }
  
  private final Interpreter mInterpreter;
}