package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

public class ValuePattern implements Pattern {
  public ValuePattern(Expr value) {
    mValue = value;
  }
  
  public Expr getValue() { return mValue; }
  
  public Expr createPredicate(Expr value) {
    return Expr.message(null, Name.EQEQ, Expr.tuple(value, mValue));
  }

  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    return mValue.toString();
  }

  private final Expr mValue;
}