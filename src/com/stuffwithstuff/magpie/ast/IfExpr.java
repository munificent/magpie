package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.Position;

public class IfExpr extends Expr {
  public IfExpr(Position position, List<Condition> conditions,
      Expr thenExpr, Expr elseExpr) {
    super(position);
    
    mConditions = conditions;
    mThen = thenExpr;
    mElse = elseExpr;
  }
  
  public List<Condition> getConditions() { return mConditions; }
  public Expr getThen() { return mThen; }
  public Expr getElse() { return mElse; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    for (Condition condition : mConditions) {
      builder.append(condition).append("\n");
    }
    builder.append("then\n")
           .append("  ").append(mThen)
           .append("else\n")
           .append("  ").append(mElse)
           .append("end");
        
    return builder.toString();
  }

  private final List<Condition> mConditions;
  private final Expr mThen;
  private final Expr mElse;
}
