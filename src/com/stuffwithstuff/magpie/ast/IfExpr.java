package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class IfExpr extends Expr {
  public IfExpr(Position position, String name, Expr condition,
      Expr thenExpr, Expr elseExpr) {
    super(position);
    
    mName = name;
    mCondition = condition;
    mThen = thenExpr;
    mElse = elseExpr;
  }
  
  public boolean isLet() { return mName != null; }
  public String getName() { return mName; }
  public Expr getCondition() { return mCondition; }
  public Expr getThen() { return mThen; }
  public Expr getElse() { return mElse; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    if (isLet()) {
      builder.append("let ").append(mName).append(" = ");
    } else {
      builder.append("if ");
    }
    builder.append(mCondition);
    builder.append("then\n").append(indent);
    mThen.toString(builder, indent + "    ");
    builder.append("else\n").append(indent);
    mElse.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }

  private final String mName;
  private final Expr mCondition;
  private final Expr mThen;
  private final Expr mElse;
}
