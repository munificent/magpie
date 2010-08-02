package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ScopeType;

public class DefineExpr extends Expr {
  public DefineExpr(ScopeType scope, String name, Expr value) {
    mScope = scope;
    mName = name;
    mValue = value;
  }
  
  public ScopeType getScope() { return mScope; }
  public String getName() { return mName; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    switch (mScope) {
    case LOCAL: builder.append("var "); break;
    case OBJECT: builder.append("def "); break;
    case CLASS: builder.append("shared "); break;
    }
    
    builder.append(mName).append(" = ").append(mValue);
    
    return builder.toString();
  }

  private final ScopeType mScope;
  private final String mName;
  private final Expr mValue;
}
