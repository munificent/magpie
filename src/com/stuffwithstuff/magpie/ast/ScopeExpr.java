package com.stuffwithstuff.magpie.ast;

public class ScopeExpr extends Expr {
  public ScopeExpr(Expr body) {
    super(body.getPosition());
    mBody = body;
  }
  
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    return mBody.toString();
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    mBody.toString(builder, indent);
  }

  private final Expr mBody;
}
