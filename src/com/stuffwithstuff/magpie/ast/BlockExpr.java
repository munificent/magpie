package com.stuffwithstuff.magpie.ast;

import java.util.*;

public class BlockExpr extends Expr {
  public BlockExpr(List<Expr> expressions) {
    mExpressions = expressions;
  }
  
  public List<Expr> getExpressions() { return mExpressions; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

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
