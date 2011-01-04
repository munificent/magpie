package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class TypePattern implements Pattern {
  public TypePattern(Expr type) {
    mType = type;
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.staticMessage(value, "is", mType);
  }
  
  public void createBindings(List<Expr> bindings, Expr root) {
    // Do nothing.
  }

  private final Expr mType;
}