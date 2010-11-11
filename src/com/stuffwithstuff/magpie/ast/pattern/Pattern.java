package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;

public interface Pattern {
  Expr createPredicate(Expr value);
}
