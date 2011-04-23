package com.stuffwithstuff.magpie.ast.pattern;

import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Name;

public abstract class Pattern {
  public static Pattern nothing() {
    return new ValuePattern(Expr.nothing());
  }
  
  public static Pattern record(Map<String, Pattern> fields) {
    return new RecordPattern(fields);
  }

  public static Pattern record(Pattern... fields) {
    Map<String, Pattern> recordFields = new HashMap<String, Pattern>();
    
    for (int i = 0; i < fields.length; i++) {
      recordFields.put(Name.getTupleField(i), fields[i]);
    }
    
    return record(recordFields);
  }

  public static Pattern type(Expr type) {
    return new TypePattern(type);
  }

  public static Pattern value(Expr value) {
    return new ValuePattern(value);
  }

  public static Pattern variable(String name) {
    return new VariablePattern(name, wildcard());
  }

  public static Pattern variable(String name, Pattern pattern) {
    return new VariablePattern(name, pattern);
  }

  public static Pattern wildcard() {
    return new WildcardPattern();
  }
  

  public abstract <R, C> R accept(PatternVisitor<R, C> visitor, C context);
}
