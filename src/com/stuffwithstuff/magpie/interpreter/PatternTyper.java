package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

public class PatternTyper implements PatternVisitor<Expr, Void> {
  public static Expr evaluate(Pattern pattern) {
    PatternTyper typer = new PatternTyper();
    
    return pattern.accept(typer, null);
  }
  
  @Override
  public Expr visit(RecordPattern pattern, Void dummy) {
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Pair<String, Pattern> field : pattern.getFields()) {
      Expr fieldExpr = field.getValue().accept(this, null);
      fields.add(new Pair<String, Expr>(field.getKey(), fieldExpr));
    }
    
    return Expr.record(Position.none(), fields);
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
  public Expr visit(ValuePattern pattern, Void dummy) {
    return Expr.message(Position.none(), pattern.getValue(), "type");
  }

  @Override
  public Expr visit(VariablePattern pattern, Void dummy) {
    // If we have a type for the variable, use it.
    if (pattern.getType() != null) {
      return pattern.getType();
    }
    
    // Otherwise, we'll match any type.
    // TODO(bob): Should this be Dynamic or Any? I think Dynamic preserves the
    // existing interpretation of fn(foo) ...
    return Expr.name("Dynamic");
  }
  
  private PatternTyper() {
  }
}
