package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class TuplePattern implements Pattern {
  public TuplePattern(List<Pattern> fields) {
    mFields = fields;
  }
  
  public List<Pattern> getFields() { return mFields; }
  
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
  
  public void createBindings(List<Pair<String, Expr>> bindings, Expr root) {
    // Give each field a chance to bind.
    for (int i = 0; i < mFields.size(); i++) {
      // Destructure the field.
      Expr field = Expr.message(root, "_" + i);
      mFields.get(i).createBindings(bindings, field);
    }
  }

  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < mFields.size(); i++) {
      builder.append(mFields.get(i));
      if (i < mFields.size() - 1) builder.append(", ");
    }
    return builder.toString();
  }
  
  private final List<Pattern> mFields;
}