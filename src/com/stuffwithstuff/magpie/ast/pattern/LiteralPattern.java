package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class LiteralPattern implements Pattern {
  public LiteralPattern(Expr value) {
    mValue = value;
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.message(null, Identifiers.EQEQ, Expr.tuple(value, mValue));
  }

  private final Expr mValue;
}