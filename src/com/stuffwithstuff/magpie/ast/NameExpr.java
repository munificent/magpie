package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;

public class NameExpr extends Expr {
  public NameExpr(Position position, String name) {
    super(position);
    mName = name;
  }
  
  public NameExpr(String name) {
    this(Position.none(), name);
  }
  
  public NameExpr(Token token) {
    this(token.getPosition(), token.getString());
  }
  
  public String getName() { return mName; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() { return mName; }

  private final String mName;
}
