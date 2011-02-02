package com.stuffwithstuff.magpie.interpreter;

import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;

public class ExprTypeParamInferrer implements ExprVisitor<Void, Obj> {
  public static void infer(Interpreter interpreter, Map<String, Obj> typeArgs,
      Expr expr, Obj valueType) {
    ExprTypeParamInferrer inferrer = new ExprTypeParamInferrer(
        interpreter, typeArgs);
    expr.accept(inferrer, valueType);
  }
  
  @Override
  public Void visit(AndExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(AssignExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(BlockExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(BoolExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(BreakExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(CallExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(FnExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(IntExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(LoopExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(MatchExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(MessageExpr expr, Obj valueType) {
    if ((expr.getReceiver() == null) &&
        mTypeArgs.containsKey(expr.getName())) {
      if (mTypeArgs.get(expr.getName()) != null) {
        // Already seen this type parameter before, so widen the type.
        Obj union = mInterpreter.orTypes(
            mTypeArgs.get(expr.getName()), valueType);
        mTypeArgs.put(expr.getName(), union);
      } else {
        mTypeArgs.put(expr.getName(), valueType);
      }
    }
    
    return null;
  }

  @Override
  public Void visit(NothingExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(OrExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(QuotationExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(RecordExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(ReturnExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(ScopeExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(StringExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(ThisExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(TupleExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(TypeofExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(UnquoteExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(UnsafeCastExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(VariableExpr expr, Obj valueType) {
    return invalidExpression();
  }
  
  private ExprTypeParamInferrer(Interpreter interpreter, Map<String, Obj> typeArgs) {
    mInterpreter = interpreter;
    mTypeArgs = typeArgs;
  }
  
  private Void invalidExpression() {
    throw new UnsupportedOperationException();
  }

  private final Interpreter mInterpreter;
  private final Map<String, Obj> mTypeArgs;
}
