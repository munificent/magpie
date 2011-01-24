package com.stuffwithstuff.magpie.interpreter;

import java.util.Map;

import com.stuffwithstuff.magpie.ast.AndExpr;
import com.stuffwithstuff.magpie.ast.ApplyExpr;
import com.stuffwithstuff.magpie.ast.AssignExpr;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.BoolExpr;
import com.stuffwithstuff.magpie.ast.BreakExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ExprVisitor;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.IntExpr;
import com.stuffwithstuff.magpie.ast.LoopExpr;
import com.stuffwithstuff.magpie.ast.MatchExpr;
import com.stuffwithstuff.magpie.ast.MessageExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.OrExpr;
import com.stuffwithstuff.magpie.ast.QuotationExpr;
import com.stuffwithstuff.magpie.ast.RecordExpr;
import com.stuffwithstuff.magpie.ast.ReturnExpr;
import com.stuffwithstuff.magpie.ast.ScopeExpr;
import com.stuffwithstuff.magpie.ast.StringExpr;
import com.stuffwithstuff.magpie.ast.ThisExpr;
import com.stuffwithstuff.magpie.ast.TupleExpr;
import com.stuffwithstuff.magpie.ast.TypeofExpr;
import com.stuffwithstuff.magpie.ast.UnquoteExpr;
import com.stuffwithstuff.magpie.ast.UnsafeCastExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;

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
  public Void visit(ApplyExpr expr, Obj valueType) {
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
  public Void visit(FnExpr expr, Obj valueType) {
    return invalidExpression();
  }

  @Override
  public Void visit(IfExpr expr, Obj valueType) {
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
