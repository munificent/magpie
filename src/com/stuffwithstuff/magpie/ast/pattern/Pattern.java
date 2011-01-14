package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;

public interface Pattern {
  // TODO(bob): Move these into visitors.
  Expr createPredicate(Expr value);
  
  <R, C> R accept(PatternVisitor<R, C> visitor, C context);
}
