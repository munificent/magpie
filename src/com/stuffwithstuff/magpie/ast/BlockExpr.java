package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.Position;

public class BlockExpr extends Expr {
  public BlockExpr(List<Expr> expressions) {
    super(Position.union(expressions.get(0).getPosition(),
        expressions.get(expressions.size() - 1).getPosition()));
    
    mExpressions = expressions;
  }
  
  public List<Expr> getExpressions() { return mExpressions; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    builder.append("do\n");
    
    for (int i = 0; i < mExpressions.size(); i++) {
      builder.append("  ").append(mExpressions.get(i)).append("\n");
    }
    builder.append("end");
    
    return builder.toString();
  }

  private final List<Expr> mExpressions;
}
