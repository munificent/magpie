package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class WildcardPattern implements Pattern {
  public WildcardPattern() {
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.bool(true);
  }
  
  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    return "_";
  }
}