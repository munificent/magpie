package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public interface Pattern {
  // TODO(bob): Move these into visitors.
  Expr createPredicate(Expr value);
  void createBindings(List<Pair<String, Expr>> bindings, Expr root);
  
  <R, C> R accept(PatternVisitor<R, C> visitor, C context);
}
