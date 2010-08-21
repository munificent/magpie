package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  public ExprChecker(Interpreter interpreter, Checker checker) {
    mInterpreter = interpreter;
    mChecker = checker;
  }
  
  public Obj checkFunction(Expr body, EvalContext context) {
    // Get the type implicitly returned by the function.
    Obj evaluatedType = body.accept(this, context);
    
    // And include any early returned types as well.
    return orTypes(evaluatedType, mReturnedTypes.toArray(new Obj[mReturnedTypes.size()]));
  }
  
  @Override
  public Obj visit(ArrayExpr expr, EvalContext context) {
    // Try to infer the element type from the contents. The rules (which may
    // change over time) are:
    // 1. An empty array has element type Dynamic.
    // 2. If all elements are == to the first, that's the element type.
    // 3. Otherwise, it's Object.
    
    // Other currently unsupported options would be:
    // For classes, look for a common base class.
    // For unrelated types, | them together.

    List<Expr> elements = expr.getElements();
    
    ExprEvaluator evaluator = new ExprEvaluator(mInterpreter);

    Obj elementType;
    if (elements.size() == 0) {
      elementType = mInterpreter.getDynamicType();
    } else {
      // Get the first element's type.
      elementType = check(elements.get(0), context);
      
      // Compare all of the others to it, if any.
      if (elements.size() > 1) {
        for (int i = 1; i < elements.size(); i++) {
          Obj other = check(elements.get(i), context);
          Obj result = evaluator.invokeMethod(expr, elementType, "==", other);
          if (!result.asBool()) {
            // No match, so default to Object.
            elementType = mInterpreter.getObjectType();
            break;
          }
        }
      }
    }
    
    Obj arrayType = mInterpreter.getArrayType();
    return evaluator.invokeMethod(expr, arrayType, "newType", elementType);
  }
  
  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    Obj left = check(expr.getLeft(), context);
    Obj right = check(expr.getRight(), context);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    EvalContext localContext = context.nestScope();
    
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
  public Obj visit(ClassExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Check the conditions for errors.
    for (Condition condition : expr.getConditions()) {
      check(condition.getBody(), context);
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
          return getMethodReturn(expr, variableType, "call", arg);
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
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj varType = check(expr.getValue(), context);

    context.define(expr.getName(), varType);
    return varType;
  }
  
  private Obj check(Expr expr, EvalContext context, boolean allowNever) {
    Obj result = expr.accept(this, context);
    
    if (!allowNever && result == mInterpreter.getNeverType()) {
      mChecker.addError(expr.getPosition(),
          "An early return here will cause unreachable code.");
    }
    
    return result;
  }
  
  private Obj check(Expr expr, EvalContext context) {
    return check(expr, context, false);
  }

  private Obj orTypes(Obj first, Obj... types) {
    Obj result = first;
    for (int i = 0; i < types.length; i++) {
      result = mChecker.invokeMethod(result, "|", types[i]);
    }
    
    return result;
  }

  public Obj getMethodReturn(Expr expr, Obj receiverType, String name,
      Obj argType) {

    Obj methodType = mChecker.invokeMethod(receiverType, "getMethodType",
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
    
    // Make sure the argument type matches the declared parameter type.
    Obj matches = mChecker.invokeMethod(paramType, "canAssignFrom", argType);
    
    if (!matches.asBool()) {
      String expectedText = mChecker.invokeMethod(paramType, "toString").asString();
      String actualText = mChecker.invokeMethod(argType, "toString").asString();
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
