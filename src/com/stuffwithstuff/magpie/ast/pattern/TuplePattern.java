package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class TuplePattern implements Pattern {
  public TuplePattern(List<Pattern> fields) {
    mFields = fields;
  }
  
  public Expr createPredicate(Expr value) {
    // All fields must match.
    Expr predicate = null;
    for (int i = 0; i < mFields.size(); i++) {
      Expr field = Expr.message(value, "_" + i);
      Expr thisPredicate = mFields.get(i).createPredicate(field);
      if (predicate == null) {
        predicate = thisPredicate;
      } else {
        predicate = Expr.and(predicate, thisPredicate);
      }
    }
    
    return predicate;
  }
  
  public void createBindings(List<Expr> bindings, Expr root) {
    // Give each field a chance to bind.
    for (int i = 0; i < mFields.size(); i++) {
      // Destructure the field.
      Expr field = Expr.message(root, "_" + i);
      mFields.get(i).createBindings(bindings, field);
    }
  }

  private final List<Pattern> mFields;
}