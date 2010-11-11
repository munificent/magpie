package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class TypePattern implements Pattern {
  public TypePattern(Expr type) {
    mType = type;
  }
  
  public Expr createPredicate(Expr value) {
    return Expr.staticMessage(value, "is", mType);
  }
  
  private final Expr mType;
}