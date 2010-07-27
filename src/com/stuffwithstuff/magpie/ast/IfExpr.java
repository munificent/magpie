package com.stuffwithstuff.magpie.ast;

import java.util.*;

public class IfExpr extends Expr {
  public IfExpr(List<Expr> conditions, Expr thenExpr, Expr elseExpr) {
    mConditions = conditions;
    mThen = thenExpr;
    mElse = elseExpr;
  }
  
  public List<Expr> getConditions() { return mConditions; }
  public Expr getThen() { return mThen; }
  public Expr getElse() { return mElse; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    for (Expr condition : mConditions) {
      builder.append("if ").append(condition).append("\n");
    }
    builder.append("then\n")
           .append("  ").append(mThen)
           .append("else\n")
           .append("  ").append(mElse)
           .append("end");
        
    return builder.toString();
  }

  private final List<Expr> mConditions;
  private final Expr mThen;
  private final Expr mElse;
}
