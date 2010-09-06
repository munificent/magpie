package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;

/**
 * Implements the visitor pattern on AST nodes. For any given expression,
 * evaluates the type of object that could result from evaluating that
 * expression. Also reports type errors as they are found.
 */
public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  public ExprChecker(Interpreter interpreter, Checker checker) {
    mInterpreter = interpreter;
    mChecker = checker;
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
    return orTypes(evaluatedType, mReturnedTypes.toArray(new Obj[mReturnedTypes.size()]));
  }
  
  public Obj check(Expr expr, EvalContext context) {
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
    Obj right = check(expr.getRight(), context);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    String name = expr.getName();
    
    // Try to assign to a local.
    Obj valueType = check(expr.getValue(), context);
    Obj existingType = context.lookUp(name);
    if (existingType != null) {
      if (context.getScope().get(name) != null) {
        // In the current scope, so just override the type.
        context.assign(name, valueType);
      } else {
        // In an outer scope, so combine the types. This handles cases where we
        // assign inside a conditional. When that occurs, the variable's type
        // may be the previous one or the new one. For example:
        //
        // var a = 123
        // if foo then a = "hi"
        // 
        // After that, a's static type will be Int | String
        Obj combinedType = orTypes(existingType, valueType);
        context.assign(name, combinedType);
      }
      
      // In either case, the assignment expression itself returns the new
      // value.
      return valueType;
    }

    // Otherwise, it must be a setter on this.
    String setter = Identifiers.makeSetter(name);
    return getMethodReturn(expr, context.getThis(), setter, valueType);
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    if (expr.createScope()) {
      context = context.nestScope();
    }
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = check(thisExpr, context);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.getBoolType();
  }
  
  @Override
  public Obj visit(BreakExpr expr, EvalContext context) {
    // The type of the break expression itself is "Never", which means an
    // expression that contains a "break" can never finish evaluating.
    return mInterpreter.getNeverType();
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // Check the body.
    mChecker.checkFunction(expr, context.getScope(), context.getThis());
    
    return mInterpreter.evaluateFunctionType(expr.getType());
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Put it in a block so that variables declared in conditions end when the
    // if expression ends.
    context = context.nestScope();
    
    // TODO(bob): Should eventually check that conditions implement ITrueable
    // so that you can only use truthy stuff in an if.
    // Check the conditions for errors.
    for (Condition condition : expr.getConditions()) {
      Obj conditionType = check(condition.getBody(), context);
      
      // If it's a "let" condition, bind and type the variable, stripping out
      // Nothing.
      if (condition.isLet()) {
        // If the condition's type evaluates to Nothing (and only Nothing), this
        // is an error, since that means we've statically determined that the
        // condition can never be entered.
        if (conditionType == mInterpreter.getNothingType()) {
          mChecker.addError(condition.getBody().getPosition(),
              "Let expression's type is Nothing which means it can never be entered.");
        }
        
        conditionType = mInterpreter.invokeMethod(conditionType,
            Identifiers.UNSAFE_REMOVE_NOTHING);
        context.define(condition.getName(), conditionType);
      }
    }
    
    // Get the types of the arms.
    Obj thenArm = check(expr.getThen(), context, true);
    Obj elseArm = check(expr.getElse(), context, true);
    
    return orTypes(thenArm, elseArm);
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
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
    Obj receiver = (expr.getReceiver() == null) ? null :
        check(expr.getReceiver(), context);
    
    Obj arg = (expr.getArg() == null) ? null :
        check(expr.getArg(), context);
    
    if (receiver == null) {
      // Just a name, so maybe it's a variable.
      Obj variableType = context.lookUp(expr.getName());
  
      if (variableType != null) {
        // If we have an argument, apply it.
        if (arg != null) {
          return getMethodReturn(expr, variableType, Identifiers.CALL, arg);
        }
        return variableType;
      }
      
      // Otherwise it must be a method on this.
      if (arg == null) arg = mInterpreter.getNothingType();
      return getMethodReturn(expr, context.getThis(), expr.getName(), arg);
    }
    
    if (arg == null) arg = mInterpreter.getNothingType();
    return getMethodReturn(expr, receiver, expr.getName(), arg);
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    // TODO(bob): Should eventually check that both arms implement ITrueable
    // so that you can only use truthy stuff in a conjunction.
    Obj left = check(expr.getLeft(), context);
    Obj right = check(expr.getRight(), context);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    // Get the type of value being returned.
    Obj returnedType = check(expr.getValue(), context);
    mReturnedTypes.add(returnedType);
    
    // The type of the return expression itself is "Never", which means an
    // expression that contains a "return" can never finish evaluating.
    return mInterpreter.getNeverType();
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
    Obj varType = check(expr.getValue(), context);

    context.define(expr.getName(), varType);
    return varType;
  }
  
  private Obj orTypes(Obj first, Obj... types) {
    Obj result = first;
    for (int i = 0; i < types.length; i++) {
      result = mInterpreter.invokeMethod(result, Identifiers.OR, types[i]);
    }
    
    return result;
  }

  public Obj getMethodReturn(Expr expr, Obj receiverType, String name,
      Obj argType) {

    Obj methodType = mInterpreter.invokeMethod(receiverType,
        Identifiers.GET_METHOD_TYPE,
        mInterpreter.createTuple(mInterpreter.createString(name), argType));
    
    if (methodType == mInterpreter.nothing()) {
      mChecker.addError(expr.getPosition(),
          "Could not find a variable or method named \"%s\" on %s when checking.",
          name, receiverType);

      return mInterpreter.getNothingType();
    }
    
    // getMethodType in Magpie is expected to return a tuple of the param and
    // return type, or nothing if the method cannot be handled by the object.
    Obj paramType = methodType.getTupleField(0);
    Obj returnType = methodType.getTupleField(1);
    
    // If the paramType is Nothing and the argType is not, then we'll treat the
    // return type as callable and call it with the argType. For example, given:
    //
    //    foo bar(123)
    //
    // If bar is declared to take Nothing then that expression is shorthand for
    //
    //    foo bar()(123)
    //
    // So we'll make that translation here.
    if ((paramType == mInterpreter.getNothingType()) &&
        (argType != mInterpreter.getNothingType())) {
      return getMethodReturn(expr, returnType, Identifiers.CALL, argType);
    }
    
    // Make sure the argument type matches the declared parameter type.
    Obj matches = mInterpreter.invokeMethod(paramType,
        Identifiers.CAN_ASSIGN_FROM, argType);
    
    if (!matches.asBool()) {
      String expectedText = mInterpreter.invokeMethod(paramType,
          Identifiers.TO_STRING).asString();
      String actualText = mInterpreter.invokeMethod(argType,
          Identifiers.TO_STRING).asString();
      mChecker.addError(expr.getPosition(),
          "Function is declared to take %s but is being passed %s.",
          expectedText, actualText);
    }
    
    // Return the return type.
    return returnType;
  }

  private final Interpreter mInterpreter;
  private final Checker mChecker;
  private final List<Obj> mReturnedTypes = new ArrayList<Obj>();
}
