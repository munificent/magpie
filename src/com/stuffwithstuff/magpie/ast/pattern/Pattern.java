package com.stuffwithstuff.magpie.ast.pattern;

public interface Pattern {
  <R, C> R accept(PatternVisitor<R, C> visitor, C context);
}
