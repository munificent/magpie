package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

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

  @Override
  public void toString(StringBuilder builder, String indent) {
    for (Condition condition : mConditions) {
      builder.append(condition).append("\n").append(indent);
    }
    builder.append("then\n").append(indent);
    mThen.toString(builder, indent + "    ");
    builder.append("else\n").append(indent);
    mElse.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }

  private final List<Condition> mConditions;
  private final Expr mThen;
  private final Expr mElse;
}
