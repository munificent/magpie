package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * This confusingly named class represents an expression that defines an
 * expression literal: a chunk of AST as a value in Magpie.
 */
public class ExpressionExpr extends Expr {
  public ExpressionExpr(Position position, Expr body) {
    super(position);
    mBody = body;
  }
  
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("{ ");
    mBody.toString(builder, indent);
    builder.append(" }");
  }

  private final Expr mBody;
}
