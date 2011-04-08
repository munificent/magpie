package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class TypePattern extends Pattern {
  TypePattern(Expr type) {
    mType = type;
  }
  
  public Expr   getType() { return mType; }
  
  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    return mType.toString();
  }

  private final Expr   mType;
}