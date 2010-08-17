package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

public class ExprChecker implements ExprVisitor<Obj, EvalContext> {
  public ExprChecker(Interpreter interpreter, Checker checker) {
    mInterpreter = interpreter;
    mChecker = checker;
  }
  
  public Obj check(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  @Override
  public Obj visit(ArrayExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
    Obj right = evaluate(expr.getRight(), context);
    
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
    Obj thenArm = check(expr.getThen(), context);
    Obj elseArm = check(expr.getElse(), context);
    
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
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
    Obj right = evaluate(expr.getRight(), context);
    
    return orTypes(left, right);
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.getStringType();
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // The type of a tuple type is just a tuple of its types.
    List<Obj> fields = new ArrayList<Obj>();
    for (Expr field : expr.getFields()) {
      fields.add(evaluate(field, context));
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
    // TODO Auto-generated method stub
    return mInterpreter.getNothingType();
  }
  
  private Obj evaluate(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  private Obj orTypes(Obj... types) {
    Obj result = types[0];
    for (int i = 1; i < types.length; i++) {
      result = mChecker.invokeMethod(result, "|", types[i]);
    }
    
    return result;
  }

  /*
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    if (expr.getTarget() == null) {
      // No target means we're just assigning to a variable (or field of this)
      // with the given name.
      String name = expr.getName();
      Obj value = check(expr.getValue(), context);
      
      // Try to assign to a local.
      Obj declared = context.lookUp(name);
      
      // If not found, try to assign to a member of this.
      if (declared == null) {
        FunctionType setter = findMethodType(context.getThis(), name + "=");
        if (setter != null) {
          declared = evaluateType(setter.getParamType());
        }
      }
      
      // Make sure the types match.
      expectType(value, declared, expr,
          "Cannot assign a %s value to a variable declared %s.",
          value, declared);
      
      return value;
    } else {
      return mInterpreter.getDynamicType();
    }
  }
  
  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    System.out.println("ClassExpr is not implemented.");
    return mInterpreter.getDynamicType();
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // Check the body of the function.
    check(expr, context);
    
    // Return the type of the function itself.
    // TODO(bob): Implement me.
    System.out.println("Function types are not implemented.");
    return mInterpreter.getDynamicType();
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    Obj receiver = check(expr.getReceiver(), context);
    Obj arg = check(expr.getArg(), context);

    return checkMethod(expr, receiver, expr.getName(), arg);
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    // TODO(bob): Implement me.
    System.out.println("Return expressions are not implemented.");
    return mInterpreter.getDynamicType();
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    return context.getThis();
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    Obj value = check(expr.getValue(), context);

    // TODO(bob): Should check for reclaring a variable.
    
    // Variables cannot be of type Nothing.
    errorIf(value == mInterpreter.getNothingType(), expr,
        "Cannot declare a variable \"%s\" of type Nothing.", expr.getName());
    
    context.define(expr.getName(), value);
    return value;
  }
  
    */
  
  private final Interpreter mInterpreter;
  private final Checker mChecker;
}
