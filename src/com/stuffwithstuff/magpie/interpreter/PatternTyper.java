package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
    for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
      Expr fieldExpr = field.getValue().accept(this, null);
      fields.add(new Pair<String, Expr>(field.getKey(), fieldExpr));
    }
    
    return Expr.record(Position.none(), fields);
  }

  @Override
  public Expr visit(TypePattern pattern, Void dummy) {
    return pattern.getType();
  }

  @Override
  public Expr visit(ValuePattern pattern, Void dummy) {
    return Expr.getter(Position.none(), pattern.getValue(), "class");
  }

  @Override
  public Expr visit(VariablePattern pattern, Void dummy) {
    return pattern.getPattern().accept(this, null);
  }

  @Override
  public Expr visit(WildcardPattern pattern, Void dummy) {
    // TODO(bob): Should this be Dynamic or Any? I think Dynamic preserves the
    // existing interpretation of fn(foo) ...
    return Expr.name("Dynamic");
  }

  private PatternTyper() {
  }
}
