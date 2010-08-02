package com.stuffwithstuff.magpie.ast;

public class DefineExpr extends Expr {
  public DefineExpr(boolean isShared, String name, Expr value) {
    mIsShared = isShared;
    mName = name;
    mValue = value;
  }
  
  public boolean isShared() { return mIsShared; }
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    if (mIsShared) {
      builder.append("shared ");
    } else {
      builder.append("def ");
    }
    
    builder.append(mName).append(" = ").append(mValue);
    
    return builder.toString();
  }

  private final boolean mIsShared;
  private final String mName;
  private final Expr mValue;
}
