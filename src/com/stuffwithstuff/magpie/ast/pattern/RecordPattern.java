package com.stuffwithstuff.magpie.ast.pattern;

import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class RecordPattern extends Pattern {
  RecordPattern(List<Pair<String, Pattern>> fields) {
    mFields = fields;
  }
  
  public List<Pair<String, Pattern>> getFields() { return mFields; }
  
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
  
  private final List<Pair<String, Pattern>> mFields;
}