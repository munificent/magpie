package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class BlockExpr extends Expr {
  public BlockExpr(Position position, List<Expr> expressions) {
    super(position);
    
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
