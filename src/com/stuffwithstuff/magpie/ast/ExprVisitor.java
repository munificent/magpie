package com.stuffwithstuff.magpie.ast;

public interface ExprVisitor<T> {
  T visit(BlockExpr expr);
  T visit(BoolExpr expr);
  T visit(CallExpr expr);
  T visit(IntExpr expr);
  T visit(MethodExpr expr);
  T visit(NameExpr expr);
  T visit(StringExpr expr);
  T visit(TupleExpr expr);
  T visit(UnitExpr expr);
}
