package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class WildcardPattern implements Pattern {
  public WildcardPattern() {
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.bool(true);
  }
}