package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.MatchCase;

public class ScopeExpr extends Expr {
  ScopeExpr(Expr body, List<MatchCase> catches) {
    super(body.getPosition());
    mBody = body;
    mCatches = catches;
  }
  
  public Expr getBody() { return mBody; }
  public List<MatchCase> getCatches() { return mCatches; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("do\n");
    mBody.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }

  private final Expr mBody;
  private final List<MatchCase> mCatches;
}
