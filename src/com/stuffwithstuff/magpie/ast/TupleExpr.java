package com.stuffwithstuff.magpie.ast;

import java.util.*;

public class TupleExpr extends Expr {
  public TupleExpr(List<Expr> fields) {
    mFields = fields;
  }
  
  public List<Expr> getFields() { return mFields; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    for (int i = 0; i < mFields.size(); i++) {
      builder.append(mFields.get(i));
      if (i < mFields.size() - 1) builder.append(", ");
    }
    
    return builder.toString();
  }

  private final List<Expr> mFields;
}
