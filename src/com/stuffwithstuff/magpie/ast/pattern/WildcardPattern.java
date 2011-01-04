package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class WildcardPattern implements Pattern {
  public WildcardPattern() {
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.bool(true);
  }
  
  public void createBindings(List<Expr> bindings, Expr root) {
    // Do nothing.
  }
}