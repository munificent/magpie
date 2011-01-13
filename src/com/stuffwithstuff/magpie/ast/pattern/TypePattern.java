package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class TypePattern implements Pattern {
  public TypePattern(Expr type) {
    mType = type;
  }
  
  public Expr getType() { return mType; }

  public Expr createPredicate(Expr value) {
    return Expr.staticMessage(value, "is", mType);
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
    return mType.toString();
  }
  
  private final Expr mType;
}