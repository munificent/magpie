package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node class for applying arguments to a static function.
 */
public class InstantiateExpr extends Expr {
  public InstantiateExpr(Position position, Expr fn, Expr arg) {
    super(position);
    
    mFn = fn;
    mArg = arg;
  }
  
  public Expr getFn()  { return mFn; }
  public Expr getArg() { return mArg; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  @Override
  public void toString(StringBuilder builder, String indent) {
    // TODO(bob): This syntax is completely temporary!
    builder.append("[ ");
    
    mFn.toString(builder, indent);
    builder.append(" : ");
    mArg.toString(builder, indent);
    builder.append(" ]");
  }

  private final Expr mFn;
  private final Expr mArg;
}
