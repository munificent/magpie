package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;

public interface Pattern {
  Expr createPredicate(Expr value);
  void createBindings(List<Expr> bindings, Expr root);
}
