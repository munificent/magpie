package com.stuffwithstuff.magpie.ast.pattern;

import java.util.Arrays;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class Pattern {
  public static Pattern nothing() {
    return new ValuePattern(Expr.nothing());
  }
  
  public static Pattern record(List<Pair<String, Pattern>> fields) {
    return new RecordPattern(fields);
  }

  public static Pattern tuple(List<Pattern> fields) {
    return new TuplePattern(fields);
  }

  public static Pattern tuple(Pattern... fields) {
    return new TuplePattern(Arrays.asList(fields));
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
