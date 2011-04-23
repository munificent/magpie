package com.stuffwithstuff.magpie.ast.pattern;

import java.util.Map;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class RecordPattern extends Pattern {
  RecordPattern(Map<String, Pattern> fields) {
    mFields = fields;
  }
  
  public Map<String, Pattern> getFields() { return mFields; }
  
  @Override
  public <R, C> R accept(PatternVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    
    boolean first = true;
    for (Entry<String, Pattern> field : mFields.entrySet()) {
      if (!first) builder.append(", ");
      first = false;
      
      builder.append(field.getKey()).append(": ").append(field.getValue());
    }

    builder.append(")");
    return builder.toString();
  }
  
  private final Map<String, Pattern> mFields;
}