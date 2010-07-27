package com.stuffwithstuff.magpie.ast;

public class DefineExpr extends Expr {
  public DefineExpr(boolean isMutable, String name, Expr value) {
    mIsMutable = isMutable;
    mName = name;
    mValue = value;
  }
  
  public boolean isMutable() { return mIsMutable; }
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    if (mIsMutable) {
      builder.append("var ");
    } else {
      builder.append("def ");
    }
    
    builder.append(mName).append(" ").append(mValue);
    
    return builder.toString();
  }

  private final boolean mIsMutable;
  private final String mName;
  private final Expr mValue;
}
