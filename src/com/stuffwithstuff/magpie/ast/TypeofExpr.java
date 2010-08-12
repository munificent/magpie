package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public class TypeofExpr extends Expr {
  public TypeofExpr(Position position, Expr body) {
    super(position);
    mBody = body;
  }
  
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return "typeof " + mBody.toString();
  }
  
  private final Expr mBody;
}
