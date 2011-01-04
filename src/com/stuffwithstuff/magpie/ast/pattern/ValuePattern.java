package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

public class ValuePattern implements Pattern {
  public ValuePattern(Expr value) {
    mValue = value;
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.message(null, Name.EQEQ, Expr.tuple(value, mValue));
  }
  
  public void createBindings(List<Expr> bindings, Expr root) {
    // Do nothing.
  }

  private final Expr mValue;
}