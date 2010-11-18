package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.util.Pair;

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
  public Obj visit(ApplyExpr expr, EvalContext context) {
    Obj target = evaluate(expr.getTarget(), context);
    Obj arg = evaluate(expr.getArg(), context);
    
    return mInterpreter.apply(expr.getPosition(), target, arg);
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    Obj receiver = evaluate(expr.getReceiver(), context);
    Obj value = evaluate(expr.getValue(), context);    

    if (receiver == null) {
      // Just a name, so maybe it's a local variable.
      if (context.assign(expr.getName(), value)) return value;
      // Otherwise it must be a property on this.
      receiver = context.getThis();
    }
    
    // Look for a setter.
    Callable setter = receiver.getClassObj().findSetter(expr.getName());
    if (setter == null) {
      mInterpreter.runtimeError(expr.getPosition(),
          "Could not find a setter \"%s\" on %s.",
          expr.getName(), receiver.getClassObj());
      
      return value;
    }
    
    setter.invoke(mInterpreter, receiver, value);
    return value;
  }
  
  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    try {
      Obj result = null;
      
      // Evaluate all of the expressions and return the last.
      for (Expr thisExpr : expr.getExpressions()) {
        result = evaluate(thisExpr, context);
      }
      
      return result;
    } catch (ErrorException err) {
      // TODO(bob): Really hokey implementation.
      Expr catchExpr = expr.getCatch();
      if (catchExpr != null) {
        // The catch expression expects to be evaluated in its own scope where
        // __err__ is bound to the exception value.
        context = context.pushScope();
        context.define("__err__", err.getError());
        return evaluate(catchExpr, context);
      } else {
        // Not caught here, so just keep unwinding.
        throw err;
      }
    }
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.createBool(expr.getValue());
  }

  @Override
  public Obj visit(BreakExpr expr, EvalContext context) {
    // Outside of a loop, "break" does nothing.
    if (context.isInLoop()) {
      throw new BreakException();
    }
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(ExpressionExpr expr, EvalContext context) {
    return mInterpreter.getExpressionType().instantiate(expr.getBody());
  }
  
  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return mInterpreter.createFn(expr, context);
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Put it in a block so that variables declared in conditions end when the
    // if expression ends.
    context = context.pushScope();
    
    // Evaluate the condition.
    boolean passed = true;
    Obj result = evaluate(expr.getCondition(), context);
    if (expr.isLet()) {
      // "let" condition.
      // If it evaluates to nothing, the condition fails. Otherwise, bind the
      // result to a name and continue.
      if (result != mInterpreter.nothing()) {
        // Success, bind the result.
        context.define(expr.getName(), result);
      } else {
        // Condition failed.
        passed = false;
      }
    } else {
      // Regular "if" condition.
      passed = isTruthy(expr, result);
    }
    
    // Evaluate the body.
    if (passed) {
      return evaluate(expr.getThen(), context);
    } else {
      return evaluate(expr.getElse(), context);
    }
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    try {
      context = context.enterLoop();
      
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
        
        // Evaluate the body in its own scope.
        context = context.pushScope();
        
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
    
    if (receiver == null) {
      // Just a name, so maybe it's a variable.
      Obj variable = context.lookUp(expr.getName());
      if (variable != null) return variable;
      
      // Otherwise it must be a property on this.
      receiver = context.getThis();
    }
    
    return mInterpreter.getProperty(expr.getPosition(), receiver,
        expr.getName());
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
  public Obj visit(RecordExpr expr, EvalContext context) {
    Obj obj = mInterpreter.getRecordType().instantiate();
    
    // Define the fields.
    for (Pair<String, Expr> entry : expr.getFields()) {
      Obj value = evaluate(entry.getValue(), context);
      obj.setField(entry.getKey(), value);
    }
    
    return obj;
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);
    throw new ReturnException(value);
  }
  
  @Override
  public Obj visit(ScopeExpr expr, EvalContext context) {
    context = context.pushScope();
    return evaluate(expr.getBody(), context);
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
  public Obj visit(UnsafeCastExpr expr, EvalContext context) {
    // No type-checking at all, just yield the value.
    return evaluate(expr.getValue(), context);
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    if (context.lookUpHere(expr.getName()) == null) {
      context.define(expr.getName(), value);
      return value;
    } else {
      // Cannot redefine a variable in the same scope.
      return mInterpreter.throwError("RedefinitionError");
    }
  }
  
  private boolean isTruthy(Expr expr, Obj receiver) {
    Obj truthy = mInterpreter.getProperty(expr.getPosition(), receiver,
        Identifiers.IS_TRUE);
    return truthy.asBool();
  }

  private final Interpreter mInterpreter;
}