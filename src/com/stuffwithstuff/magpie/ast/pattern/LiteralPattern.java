package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

public class LiteralPattern implements Pattern {
  public LiteralPattern(Expr value) {
    mValue = value;
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.message(null, Name.EQEQ, Expr.tuple(value, mValue));
  }

  private final Expr mValue;
}