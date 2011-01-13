package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class VariablePattern implements Pattern {
  /**
   * A pattern that binds a variable. It may optionally require a pattern
   * to also match, for example "a Int" where "Int" is the pattern.
   * 
   * @param name
   * @param pattern
   */
  public VariablePattern(String name, Pattern pattern) {
    mName = name;
    mPattern = pattern;
  }
  
  public String getName() { return mName; }
  public Pattern getPattern() { return mPattern; }
  
  public Expr createPredicate(Expr value) {
    if (mPattern != null) {
      return mPattern.createPredicate(value);
    } else {
      return Expr.bool(true);
    }
  }
  
  public void createBindings(List<Pair<String, Expr>> bindings, Expr root) {
    bindings.add(new Pair<String, Expr>(mName, root));
  }

  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    if (mPattern == null) return mName;
    return mName + " " + mPattern.toString();
  }

  private final String mName;
  private final Pattern mPattern;
}