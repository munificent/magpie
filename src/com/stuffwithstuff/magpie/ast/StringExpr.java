package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Token;

public class StringExpr extends Expr {
  public StringExpr(Token token) {
    super(token.getPosition());
    mValue = token.getString();
  }
  
  public String getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() { return mValue; }

  private final String mValue;
}
