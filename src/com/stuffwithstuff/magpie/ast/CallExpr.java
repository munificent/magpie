package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for a function call: applies an argument to a function-like target.
 */
public class CallExpr extends Expr {
  CallExpr(Expr target, Expr arg) {
    super(Position.surrounding(target, arg));
    mTarget = target;
    mArg = arg;
  }
  
  public Expr getTarget() { return mTarget; }
  public Expr getArg() { return mArg; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    mTarget.toString(builder, indent);
    builder.append("(");
    mArg.toString(builder, indent);
    builder.append(")");
  }

  private final Expr mTarget;
  private final Expr mArg;
}
