package com.stuffwithstuff.magpie.ast.pattern;

import java.util.Arrays;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class Pattern {
  public static Pattern nothing() {
    return new ValuePattern(Expr.nothing());
  }
  
  public static RecordPattern record(List<Pair<String, Pattern>> fields) {
    return new RecordPattern(fields);
  }

  public static TuplePattern tuple(List<Pattern> fields) {
    return new TuplePattern(fields);
  }

  public static TuplePattern tuple(Pattern... fields) {
    return new TuplePattern(Arrays.asList(fields));
  }

  public static VariablePattern type(Expr type) {
    return new VariablePattern("_", type);
  }

  public static ValuePattern value(Expr value) {
    return new ValuePattern(value);
  }

  public static VariablePattern variable(String name) {
    return new VariablePattern(name, null);
  }

  public static VariablePattern variable(String name, Expr type) {
    return new VariablePattern(name, type);
  }

  public static VariablePattern wildcard() {
    return new VariablePattern("_", null);
  }
  

  public abstract <R, C> R accept(PatternVisitor<R, C> visitor, C context);
}
