package com.stuffwithstuff.magpie.ast.pattern;

public interface PatternVisitor<R, C> {
  R visit(TuplePattern pattern, C context);
  R visit(TypePattern pattern, C context);
  R visit(ValuePattern pattern, C context);
  R visit(VariablePattern pattern, C context);
  R visit(WildcardPattern pattern, C context);
}
