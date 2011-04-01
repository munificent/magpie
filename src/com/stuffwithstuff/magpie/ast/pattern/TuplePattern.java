package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class TuplePattern extends Pattern {
  TuplePattern(List<Pattern> fields) {
    mFields = fields;
  }
  
  public List<Pattern> getFields() { return mFields; }
  
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