package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;

public class StringExpr extends Expr {
  public StringExpr(Position position, String text) {
    super(position);
    mValue = text;
  }
  
  public StringExpr(Token token) {
    this(token.getPosition(), token.getString());
  }
  
  public String getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("\"").append(mValue).append("\"");
  }

  private final String mValue;
}
