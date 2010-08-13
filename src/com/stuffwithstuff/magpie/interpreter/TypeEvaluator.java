package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.*;

public class TypeEvaluator implements ExprVisitor<Obj, EvalContext> {

  public static Obj evaluate(Interpreter interpreter, Expr expr,
      EvalContext context) {
    
    TypeEvaluator evaluator = new TypeEvaluator(interpreter);
    return expr.accept(evaluator, context);
  }
  
  private TypeEvaluator(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  @Override
  public Obj visit(ArrayExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    // An assignment returns the value being assigned.
    return evaluate(expr.getValue(), context);
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    // TODO(bob): This is maybe a little fishy. We're creating a new context
    // that will hold types now and not values. Do we need to explicitly split
    // these up in the interpreter?
    EvalContext localContext = context.newBlockScope();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, localContext);
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
    return null;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    // Right now, loops can't evaluate to anything.
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    return mInterpreter.getNeverType();
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.getStringType();
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(TypeofExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    // Variable definitions return the defined value.
    return evaluate(expr.getValue(), context);
  }
  
  private Obj evaluate(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
    
  private final Interpreter mInterpreter;
}
