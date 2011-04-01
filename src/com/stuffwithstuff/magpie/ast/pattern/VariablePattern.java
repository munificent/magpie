package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class VariablePattern extends Pattern {
  /**
   * A pattern that binds a variable. It may optionally require a pattern
   * to also match, for example "a Int" where "Int" is the pattern.
   * 
   * @param name
   * @param pattern
   */
  VariablePattern(String name, Expr type) {
    mName = name;
    mType = type;
  }
  
  public String getName() { return mName; }
  public Expr   getType() { return mType; }
  
  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    if (mType == null) return mName;
    return mName + " " + mType.toString();
  }

  private final String mName;
  private final Expr   mType;
}