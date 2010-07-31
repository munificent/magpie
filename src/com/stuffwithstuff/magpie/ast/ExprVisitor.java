package com.stuffwithstuff.magpie.ast;

public interface ExprVisitor<R, C> {
  R visit(AssignExpr expr, C context);
  R visit(BlockExpr expr, C context);
  R visit(BoolExpr expr, C context);
  R visit(CallExpr expr, C context);
  R visit(ClassExpr expr, C context);
  R visit(DefineExpr expr, C context);
  R visit(FnExpr expr, C context);
  R visit(IfExpr expr, C context);
  R visit(IntExpr expr, C context);
  R visit(LoopExpr expr, C context);
  R visit(MethodExpr expr, C context);
  R visit(NameExpr expr, C context);
  R visit(NothingExpr expr, C context);
  R visit(StringExpr expr, C context);
  R visit(ThisExpr expr, C context);
  R visit(TupleExpr expr, C context);
}
