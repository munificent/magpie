package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Implements the visitor pattern on AST nodes. For any given expression,
 * evaluates the type of object that could result from evaluating that
 * expression. Also reports type errors as they are found.
 */
public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  public ExprChecker(Interpreter interpreter, Checker checker, EvalContext
      staticContext) {
    mInterpreter = interpreter;
    mChecker = checker;
    mStaticContext = staticContext;
  }
  
  /**
   * Checks the given function body for a function declared within the given
   * lexical context. Returns the type returned by a call to that function. This
   * type will include the type of the expression body itself, combined with the
   * types returned by any "return" expressions found in the function.
   * 
   * @param body     The body of the function.
   * @param context  The type context where the function was declared.
   * @return         The type of object returned by a call to this function.
   */
  public Obj checkFunction(Expr body, EvalContext context) {
    // Get the type implicitly returned by the function.
    Obj evaluatedType = body.accept(this, context);
    
    // And include any early returned types as well.
    for (int i = 0; i < mReturnedTypes.size(); i++) {
      evaluatedType = orTypes(evaluatedType, mReturnedTypes.get(i));
    }
    return evaluatedType;
  }
  
  public Obj check(Expr expr, EvalContext context) {
    if (expr == null) return null;
    return check(expr, context, false);
  }

  public Obj check(Expr expr, EvalContext context, boolean allowNever) {
    Obj result = expr.accept(this, context);
    
    if (!allowNever && result == mInterpreter.getNeverType()) {
      mChecker.addError(expr.getPosition(),
          "An early return here will cause unreachable code.");
    }
    
    return result;
  }

  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    // TODO(bob): Should eventually check that both arms implement ITrueable
    // so that you can only use truthy stuff in a conjunction.
    Obj left = check(expr.getLeft(), context);
    Obj right = check(expr.getRight(), context, true);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(ApplyExpr expr, EvalContext context) {
    Obj targetType = check(expr.getTarget(), context);
    Obj argType = check(expr.getArg(), context);
    
    // If the target is not an actual function, get the type of its "call"
    // message instead of the target itself.
    // See if the target is an actual function, or a functor.
    // TODO(bob): Using the class name for this is gross!
    if (!targetType.getClassObj().getName().equals(Identifiers.FUNCTION_TYPE)) {
      // It's a functor, so look up the "call" member.
      targetType = getMemberType(expr.getPosition(), targetType,
          Identifiers.CALL);

      if (targetType == mInterpreter.nothing()) {
        mChecker.addError(expr.getPosition(),
            "Target of type %s is not a function and does not have a 'call' method.",
            targetType);
        return mInterpreter.getNothingType();
      }
    }
    
    Obj paramType = targetType.getField(Identifiers.PARAM_TYPE);
    Obj returnType = targetType.getField(Identifiers.RETURN_TYPE);
    
    // Make sure the argument type matches the declared parameter type.
    mChecker.checkTypes(paramType, argType, expr.getPosition(), 
        "Function is declared to take %s but is being passed %s.");
    
    // Calling a function results in the function's return type.
    return returnType;
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    Obj receiverType = check(expr.getReceiver(), context);
    Obj valueType = check(expr.getValue(), context);    

    if (receiverType == null) {
      // Just a name, so maybe it's a local variable.
      Obj existingType = context.lookUp(expr.getName());
      if (existingType != null) {
        
        // Make sure the new value is compatible with the variable's type.
        mChecker.checkTypes(existingType, valueType, expr.getPosition(),
            "Variable of type %s cannot be assigned a value of type %s.");

        // The type doesn't change.
        return existingType;
      }
      
      // Otherwise it must be a property on this.
      receiverType = context.getThis();
    }
    
    Obj setterType = mInterpreter.invokeMethod(receiverType,
        Identifiers.GET_SETTER_TYPE, mInterpreter.createString(expr.getName()));
    
    if (setterType == mInterpreter.nothing()) {
      mChecker.addError(expr.getPosition(),
          "Could not find a setter \"%s\" on %s when checking.",
          expr.getName(), receiverType);

      return mInterpreter.getNothingType();
    }
    
    // Make sure the assigned value if compatible with the setter.
    mChecker.checkTypes(setterType, valueType, expr.getPosition(),
        "Setter of type %s cannot be assigned a value of type %s.");

    return setterType;
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    if (expr.createScope()) {
      context = context.pushScope();
    }
    
    // Evaluate all of the expressions and return the last.
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      Expr thisExpr = expr.getExpressions().get(i);
      // Can only early return from the end of a block.
      boolean allowReturn = i == expr.getExpressions().size() - 1;
      result = check(thisExpr, context, allowReturn);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.getBoolType();
  }
  
  @Override
  public Obj visit(BreakExpr expr, EvalContext context) {
    if (context.isInLoop()) {
      return mInterpreter.getNeverType();
    }
    
    mChecker.addError(expr.getPosition(),
        "A break expression should not appear outside of a for loop.");
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(ExpressionExpr expr, EvalContext context) {
    return mInterpreter.getExpressionType();
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // Check the body and create a context containing any static arguments the
    // function defines.
    return mChecker.checkFunction(expr, context.getScope(), context.getThis(),
        mStaticContext);
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Put it in a block so that variables declared in conditions end when the
    // if expression ends.
    context = context.pushScope();
    
    // TODO(bob): Should eventually check that conditions implement ITrueable
    // so that you can only use truthy stuff in an if.
    // Check the condition for errors.
    Obj conditionType = check(expr.getCondition(), context);
    
    // If it's a "let" condition, bind and type the variable, stripping out
    // Nothing.
    if (expr.isLet()) {
      conditionType = mInterpreter.invokeMethod(conditionType,
          Identifiers.UNSAFE_REMOVE_NOTHING, mInterpreter.nothing());
      context.define(expr.getName(), conditionType);
    }
    
    // Get the types of the arms.
    Obj thenArm = check(expr.getThen(), context, true);
    Obj elseArm = check(expr.getElse(), context, true);
    
    return orTypes(thenArm, elseArm);
  }

  @Override
  public Obj visit(InstantiateExpr expr, EvalContext context) {
    // TODO(bob): Almost all of this is copied from ExprEvaluator. Should unify.
    // Evaluate the static argument.
    Obj fn = mInterpreter.evaluate(expr.getFn(), context);
    Obj arg = mInterpreter.evaluate(expr.getArg(), mStaticContext);
    
    if (!(fn.getValue() instanceof StaticFnExpr)) {
      mChecker.addError(expr.getFn().getPosition(),
          "The expression \"%s\" does not evaluate to a static function.",
          expr.getFn());
      return mInterpreter.getNothingType();
    }
    
    StaticFnExpr staticFn = (StaticFnExpr)fn.getValue();
    
    // Push a new static context with the bound static arguments.
    EvalContext staticContext = mStaticContext.pushScope();
    
    // Bind the argument(s) to the static parameter(s). Note that the names are
    // bound in both the static context and the regular type context. The former
    // is so that static arguments can be used in subsequent type annotations
    // (this is the motivation to even *have* static functions). The latter is
    // so that the static arguments are also available at runtime.
    if (staticFn.getParams().size() > 1) {
      // TODO(bob): Gross, assume arg is a tuple.
      for (int i = 0; i < staticFn.getParams().size(); i++) {
        staticContext.define(staticFn.getParams().get(i), arg.getTupleField(i));
        context.define(staticFn.getParams().get(i), arg.getTupleField(i));
      }
    } else if (staticFn.getParams().size() == 1) {
      staticContext.define(staticFn.getParams().get(0), arg);
      context.define(staticFn.getParams().get(0), arg);
    }
    
    // Now that we have a context where the static parameters are bound to
    // concrete values, we can check the body of the original static function.
    ExprChecker checker = new ExprChecker(mInterpreter, mChecker, staticContext);
    return checker.check(staticFn.getBody(), context);
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    context = context.enterLoop();
    
    // TODO(bob): Should eventually check that conditions implement ITrueable
    // so that you can only use truthy stuff in a while.
    // Check the conditions for errors.
    for (Expr condition : expr.getConditions()) {
      check(condition, context);
    }
    
    // Check the body for errors.
    check(expr.getBody(), context);
    
    // Loops always return nothing.
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    Obj receiver = check(expr.getReceiver(), context);
    
    if (receiver == null) {
      // Just a name, so maybe it's a variable.
      Obj variableType = context.lookUp(expr.getName());
      if (variableType != null) return variableType;
      
      // Otherwise it must be a property on this.
      receiver = context.getThis();
    }
    
    return getMemberType(expr.getPosition(), receiver, expr.getName());
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(ObjectExpr expr, EvalContext context) {
    // TODO(bob): Need to create a structural type here.
    // Also, should check for duplicate fields?
    return mInterpreter.getObjectType();
  }
  
  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    // TODO(bob): Should eventually check that both arms implement ITrueable
    // so that you can only use truthy stuff in a conjunction.
    Obj left = check(expr.getLeft(), context);
    Obj right = check(expr.getRight(), context, true);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    // Get the type of value being returned.
    Obj returnedType = check(expr.getValue(), context);
    mReturnedTypes.add(returnedType);
    
    return mInterpreter.getNeverType();
  }

  @Override
  public Obj visit(StaticFnExpr expr, EvalContext context) {
    return mInterpreter.evaluateStaticFunctionType(expr, mStaticContext);
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
    // The type of a tuple type is just a tuple of its types.
    List<Obj> fields = new ArrayList<Obj>();
    for (Expr field : expr.getFields()) {
      fields.add(check(field, context));
    }
    
    return mInterpreter.createTuple(fields);
  }

  @Override
  public Obj visit(TypeofExpr expr, EvalContext context) {
    // TODO(bob): This should eventually return IType | Nothing
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj valueType = check(expr.getValue(), context);

    // See if there is already a variable in this scope with that name. If so,
    // we'll treat this declaration like an assignment.
    Obj existingType = context.lookUpHere(expr.getName());
    if (existingType == null) {
      // Not already defined, so define it.
      context.define(expr.getName(), valueType);
      return valueType;
    } else {
      mChecker.addError(expr.getPosition(),
          "There is already a variable named \"%s\" declared in this scope.",
          expr.getName());

      // The type doesn't change.
      return existingType;
    }
  }
  
  private Obj orTypes(Obj left, Obj right) {
    // Never is omitted.
    if (left == mInterpreter.getNeverType()) return right;
    if (right == mInterpreter.getNeverType()) return left;
    
    return mInterpreter.invokeMethod(left, Identifiers.OR, right);
  }
  
  public Obj getMemberType(Position position, Obj receiverType, String name) {
    Obj memberType = mInterpreter.invokeMethod(receiverType,
        Identifiers.GET_MEMBER_TYPE, mInterpreter.createString(name));
    
    if (memberType == mInterpreter.nothing()) {
      mChecker.addError(position,
          "Could not find a member named \"%s\" on %s when checking.",
          name, receiverType);
      
      return mInterpreter.getNothingType();
    }

    return memberType;
  }

  private final Interpreter mInterpreter;
  private final Checker mChecker;
  private final List<Obj> mReturnedTypes = new ArrayList<Obj>();
  // The context in which type annotations and other static expressions are
  // evaluated during type-checking.
  // TODO(bob): Everything that evaluates during check time should use this.
  private final EvalContext mStaticContext;
}
