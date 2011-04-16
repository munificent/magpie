package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

public class RecordExpr extends Expr {
  RecordExpr(Position position, List<Pair<String, Expr>> fields) {
    super(position);
    
    mFields = fields;
  }
  
  /**
   * Gets the fields for the record. We're using a list of entries instead of a
   * map because we need to ensure that fields are evaluated in the order that
   * they appear in the source.
   */
  public List<Pair<String, Expr>> getFields() { return mFields; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    int i = 0;
    for (Pair<String, Expr> Pair : mFields) {
      if (i++ > 0) builder.append(", ");
      builder.append(Pair.getKey()).append(": ");
      Pair.getValue().toString(builder, indent);
    }
  }

  private final List<Pair<String, Expr>> mFields;
}
