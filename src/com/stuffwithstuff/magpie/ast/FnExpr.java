package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node class for an function definition.
 */
public class FnExpr extends Expr {
  public FnExpr(Position position, FunctionType type, Expr body,
      boolean isStatic) {
    super(position);
    
    mType = type;
    mBody = body;
    mIsStatic = isStatic;
  }
  
  public FunctionType getType()  { return mType; }
  public Expr         getBody()  { return mBody; }
  public boolean      isStatic() { return mIsStatic; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("fn ").append(mType).append(" ");
    mBody.toString(builder, indent);
  }

  private final FunctionType  mType;
  private final Expr          mBody;
  private final boolean       mIsStatic;
}
