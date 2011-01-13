package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.Pair;

public class ValuePattern implements Pattern {
  public ValuePattern(Expr value) {
    mValue = value;
  }
  
  public Expr getValue() { return mValue; }
  
  public Expr createPredicate(Expr value) {
    return Expr.message(null, Name.EQEQ, Expr.tuple(value, mValue));
  }
  
  public void createBindings(List<Pair<String, Expr>> bindings, Expr root) {
    // Do nothing.
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