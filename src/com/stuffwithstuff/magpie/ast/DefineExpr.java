package com.stuffwithstuff.magpie.ast;

public class DefineExpr extends Expr {
  public DefineExpr(Position position, String name, Expr value) {
    super(position);
    mName = name;
    mValue = value;
  }
  
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    builder.append("var ").append(mName).append(" = ").append(mValue);
    
    return builder.toString();
  }

  private final String mName;
  private final Expr mValue;
}
