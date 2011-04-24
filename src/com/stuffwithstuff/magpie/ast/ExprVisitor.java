package com.stuffwithstuff.magpie.ast;

public interface ExprVisitor<R, C> {
  R visit(AssignExpr expr, C context);
  R visit(SequenceExpr expr, C context);
  R visit(BoolExpr expr, C context);
  R visit(BreakExpr expr, C context);
  R visit(CallExpr expr, C context);
  R visit(ClassExpr expr, C context);
  R visit(FnExpr expr, C context);
  R visit(ImportExpr expr, C context);
  R visit(IntExpr expr, C context);
  R visit(ListExpr expr, C context);
  R visit(LoopExpr expr, C context);
  R visit(MatchExpr expr, C context);
  R visit(VariableExpr expr, C context);
  R visit(MethodExpr expr, C context);
  R visit(NothingExpr expr, C context);
  R visit(RecordExpr expr, C context);
  R visit(ReturnExpr expr, C context);
  R visit(ScopeExpr expr, C context);
  R visit(StringExpr expr, C context);
  R visit(ThrowExpr expr, C context);
  R visit(DefineExpr expr, C context);
}
