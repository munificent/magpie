package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
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
    Member setter = ClassObj.findMember(null, receiver, expr.getName() + "_=");
    if (setter == null) {
      mInterpreter.runtimeError(expr.getPosition(),
          "Could not find a setter \"%s\" on %s.",
          expr.getName(), receiver.getClassObj());

      return value;
    }

    setter.getDefinition().invoke(mInterpreter, receiver, null, value);
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
  public Obj visit(CallExpr expr, EvalContext context) {
    Obj target = evaluate(expr.getTarget(), context);
    
    List<Obj> typeArgs = new ArrayList<Obj>();
    for (Expr typeArg : expr.getTypeArgs()) {
      typeArgs.add(evaluate(typeArg, context));
    }
    
    Obj arg = evaluate(expr.getArg(), context);

    return mInterpreter.apply(expr.getPosition(), target, typeArgs, arg);
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return mInterpreter.createFn(expr, context);
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    try {
      context = context.enterLoop();

      // Loop forever. A "break" expression will throw a BreakException to
      // escape this loop.
      while (true) {
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
  public Obj visit(MatchExpr expr, EvalContext context) {
    // Push a new context so that a variable declared in the value expression
    // itself disappears after the match, i.e.:
    // match var i = 123
    // ...
    // end
    // i should be gone here
    context = context.pushScope();
    
    Obj value = evaluate(expr.getValue(), context);
    
    // Try each pattern until we get a match.
    for (MatchCase matchCase : expr.getCases()) {
      Pattern pattern = matchCase.getPattern();
      if (PatternTester.test(mInterpreter, pattern, value, context)) {
        // Matched. Bind variables and evaluate the body.
        context = context.pushScope();
        PatternBinder.bind(mInterpreter, pattern, value, context);
        
        return evaluate(matchCase.getBody(), context);
      }
    }
    
    // If we got here, no patterns matched.
    return mInterpreter.throwError("NoMatchError");
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    Obj receiver = evaluate(expr.getReceiver(), context);

    // If there is an implicit receiver, try to determine who to send the
    // message to.
    if (receiver == null) {
      // Just a name, so maybe it's a variable.
      Obj variable = context.lookUp(expr.getName());
      if (variable != null) return variable;

      // Otherwise it must be a property on this.
      receiver = context.getThis();
    }

    return mInterpreter.getMember(expr.getPosition(), receiver, expr.getName());
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(QuotationExpr expr, EvalContext context) {
    return JavaToMagpie.convertAndUnquote(
        mInterpreter, expr.getBody(), context);
  }

  @Override
  public Obj visit(RecordExpr expr, EvalContext context) {
    // Evaluate the fields.
    Map<String, Obj> fields = new HashMap<String, Obj>();
    for (Pair<String, Expr> entry : expr.getFields()) {
      Obj value = evaluate(entry.getValue(), context);
      fields.put(entry.getKey(), value);
    }

    return mInterpreter.createRecord(fields);
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
  public Obj visit(UnquoteExpr expr, EvalContext context) {
    throw new UnsupportedOperationException(
        "An unquoted expression cannot be directly evaluated.");
  }

  @Override
  public Obj visit(UnsafeCastExpr expr, EvalContext context) {
    // No type-checking at all, just yield the value.
    return evaluate(expr.getValue(), context);
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    PatternBinder.bind(mInterpreter, expr.getPattern(), value, context);
    return value;
  }

  private final Interpreter mInterpreter;
}