package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.*;

public class PatternTyper implements PatternVisitor<Expr, Void> {
  public static Expr evaluate(Pattern pattern) {
    PatternTyper typer = new PatternTyper();
    
    return pattern.accept(typer, null);
  }
  
  @Override
  public Expr visit(TuplePattern pattern, Void dummy) {
    List<Expr> fields = new ArrayList<Expr>();
    for (Pattern fieldPattern : pattern.getFields()) {
      Expr field = fieldPattern.accept(this, null);
      fields.add(field);
    }
    
    return Expr.tuple(fields);
  }

  @Override
  public Expr visit(TypePattern pattern, Void dummy) {
    return pattern.getType();
  }

  @Override
  public Expr visit(ValuePattern pattern, Void dummy) {
    return Expr.message(pattern.getValue(), "type");
  }

  @Override
  public Expr visit(VariablePattern pattern, Void dummy) {
    // If we have a pattern for the variable, defer to its type.
    if (pattern.getPattern() != null) {
      return pattern.getPattern().accept(this, dummy);
    }
    
    // Otherwise, we'll match any type.
    // TODO(bob): Should this be Dynamic or Any? I think Dynamic preserves the
    // existing interpretation of fn(foo) ...
    return Expr.name("Dynamic");
  }

  @Override
  public Expr visit(WildcardPattern pattern, Void dummy) {
    return Expr.name("Any");
  }
  
  private PatternTyper() {
  }
}
