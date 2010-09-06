package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;

public class BoolExpr extends Expr {
  public BoolExpr(Position position, boolean value) {
    super(position);
    mValue = value;
  }

  public BoolExpr(Token token) {
    this(token.getPosition(), token.getBool());
  }
  
  public boolean getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(mValue);
  }

  private final boolean mValue;
}
