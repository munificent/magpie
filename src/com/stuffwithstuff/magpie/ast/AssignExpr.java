package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.Position;

public class AssignExpr extends Expr {
  public AssignExpr(Position position, Expr target, String name, Expr targetArg, Expr value) {
    super(position);
    
    mTarget = target;
    mName = name;
    mTargetArg = targetArg;
    mValue = value;
  }
  
  public Expr getTarget() { return mTarget; }
  public String getName() { return mName; }
  public Expr getTargetArg() { return mTargetArg; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return mName + " = " + mValue;
  }

  private final Expr mTarget;
  private final String mName;
  private final Expr mTargetArg;
  private final Expr mValue;
}
