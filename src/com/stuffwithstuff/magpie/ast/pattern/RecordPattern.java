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
    builder.append("(");
    for (int i = 0; i < mFields.size(); i++) {
      Pair<String, Pattern> field = mFields.get(i);
      if (i > 0) builder.append(", ");
      builder.append(field.getKey()).append(": ").append(field.getValue());
    }
    builder.append(")");
    return builder.toString();
  }
  
  private final List<Pair<String, Pattern>> mFields;
}