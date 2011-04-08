package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class WildcardPattern extends Pattern {
  WildcardPattern() {
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