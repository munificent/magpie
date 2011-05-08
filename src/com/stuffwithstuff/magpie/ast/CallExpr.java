package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * A call to a multimethod. Includes method calls, getters, and setters.
 */
public class CallExpr extends Expr {
  CallExpr(Position position, String name, Expr arg) {
    super(position);

    mName = name;
    mArg = arg;
  }

  public String getName()      { return mName; }
  public Expr   getArg()       { return mArg; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(mName);
    
    if (mArg instanceof NothingExpr) {
      builder.append("()");
    } else {
      builder.append("(").append(mArg).append(")");
    }
  }

  private final String mName;
  private final Expr mArg;
}
