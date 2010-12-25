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
    builder.append("do\n");
    mBody.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }

  private final Expr mBody;
}
