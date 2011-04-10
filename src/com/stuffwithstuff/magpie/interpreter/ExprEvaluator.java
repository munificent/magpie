package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.NotImplementedException;
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
    Obj value = evaluate(expr.getValue(), context);

    // See if it's an assignment to a local variable.
    if (context.assign(expr.getName(), value)) return value;
    
    // Otherwise it must be a property on this.
    // TODO(bob): Need to handle implicit "this" on setters. :(
    throw new NotImplementedException();
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
      // See if we can catch it here.
      Obj result = this.evaluateCases(err.getError(), expr.getCatches(), context);
      if (result != null) return result;

      // Not caught here, so just keep unwinding.
      throw err;
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
    Multimethod multimethod = context.lookUpMultimethod(expr.getName());
    if (multimethod == null) throw mInterpreter.error("NoMethodError");

    // Figure out the receiver.
    Obj receiver;
    if (expr.getReceiver() == null) {
      receiver = mInterpreter.nothing();
    } else {
      receiver = evaluate(expr.getReceiver(), context);
    }
    
    // If we're given a right-hand argument, combine it with the receiver.
    // If not, this is a getter-style multimethod.
    Obj arg;
    if (expr.getArg() != null) {
      arg = mInterpreter.createTuple(receiver, evaluate(expr.getArg(), context));
    } else {
      arg = receiver;
    }

    return multimethod.invoke(mInterpreter, arg);
  }
  
  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // Look up the parents.
    List<ClassObj> parents = new ArrayList<ClassObj>();
    for (String parentName : expr.getParents()) {
      parents.add(context.lookUp(parentName).asClass());
    }
    
    ClassObj classObj = mInterpreter.createClass(expr.getName(), parents,
        expr.getFields(), context.getScope());
    
    context.define(expr.getName(), classObj);
    
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    PatternBinder.bind(mInterpreter, expr.getPattern(), value, context);
    return value;
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
  public Obj visit(ListExpr expr, EvalContext context) {
    // Evaluate the elements.
    List<Obj> elements = new ArrayList<Obj>();
    for (Expr element : expr.getElements()) {
      elements.add(evaluate(element, context));
    }

    return mInterpreter.createList(elements);
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
    Obj result = evaluateCases(value, expr.getCases(), context);
    if (result != null) return result;
    
    // If we got here, no patterns matched.
    throw mInterpreter.error("NoMatchError");
  }
  
  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    // Look for a local variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    throw mInterpreter.error("UnknownVariableError",
        "Could not find a variable named \"" + expr.getName() + "\".");
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    Multimethod multimethod = context.getScope().getMultimethod(expr.getName());
    if (multimethod == null) {
      multimethod = new Multimethod();
      context.getScope().defineMultimethod(expr.getName(), multimethod);
    }
    
    Function function = new Function(
        Expr.fn(expr.getPosition(), expr.getPattern(), expr.getBody()),
        context.getScope());
    multimethod.addMethod(function);
    
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.nothing();
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
  public Obj visit(TupleExpr expr, EvalContext context) {
    // Evaluate the fields.
    Obj[] fields = new Obj[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = evaluate(expr.getFields().get(i), context);
    }

    return mInterpreter.createTuple(fields);
  }

  private Obj evaluateCases(Obj value, List<MatchCase> cases,
      EvalContext context) {
    if (cases == null) return null;
    
    for (MatchCase matchCase : cases) {
      Pattern pattern = matchCase.getPattern();
      if (PatternTester.test(mInterpreter, pattern, value, context)) {
        // Matched. Bind variables and evaluate the body.
        context = context.pushScope();
        PatternBinder.bind(mInterpreter, pattern, value, context);
        
        return evaluate(matchCase.getBody(), context);
      }
    }
    
    return null;
  }

  private final Interpreter mInterpreter;
}