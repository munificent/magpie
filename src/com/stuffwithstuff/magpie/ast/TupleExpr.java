package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class TupleExpr extends Expr {
  public TupleExpr(List<Expr> fields) {
    super(Position.surrounding(fields));
    
    mFields = fields;
  }
  
  public TupleExpr(Expr... fields) {
    this(Arrays.asList(fields));
  }
  
  public List<Expr> getFields() { return mFields; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("(");
    for (int i = 0; i < mFields.size(); i++) {
      mFields.get(i).toString(builder, indent);
      if (i < mFields.size() - 1) builder.append(", ");
    }
    builder.append(")");
  }

  private final List<Expr> mFields;
}
