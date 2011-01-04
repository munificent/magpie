package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

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
  
  public Expr createPredicate(Expr value) {
    if (mPattern != null) {
      return mPattern.createPredicate(value);
    } else {
      return Expr.bool(true);
    }
  }
  
  public void createBindings(List<Expr> bindings, Expr root) {
    bindings.add(Expr.var(mName, root));
  }

  private final String mName;
  private final Pattern mPattern;
}