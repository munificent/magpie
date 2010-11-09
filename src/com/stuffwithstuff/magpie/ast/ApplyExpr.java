package com.stuffwithstuff.magpie.ast;

/**
 * AST node for a function application: applies an argument to a function-like
 * target.
 */
public class ApplyExpr extends Expr {
  public ApplyExpr(Expr target, Expr arg, boolean isStatic) {
    super(target.getPosition().union(arg.getPosition()));
    mTarget = target;
    mArg = arg;
    mIsStatic = isStatic;
  }
  
  public Expr getTarget() { return mTarget; }
  public Expr getArg() { return mArg; }
  public boolean isStatic() { return mIsStatic; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    mTarget.toString(builder, indent);
    builder.append(mIsStatic ? "[" : "(");
    mArg.toString(builder, indent);
    builder.append(mIsStatic ? "]" : ")");
  }

  private final Expr mTarget;
  private final Expr mArg;
  private final boolean mIsStatic;
}
