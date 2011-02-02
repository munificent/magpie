package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

// TODO(bob): Instead of supporting "let" directly, that should just desugar to
// more primitive if expression using unsafecast.
// TODO(bob): Note that this is *only* used for let expressions now. Regular
// if expressions are just desugared to a match. Get rid of this completely if
// we can figure out how to desugar let expressions to matches too.
public class IfExpr extends Expr {
  public IfExpr(Position position, String name, Expr condition,
      Expr thenExpr, Expr elseExpr) {
    super(position);
    
    mName = name;
    mCondition = condition;
    mThen = thenExpr;
    mElse = elseExpr;
  }
  
  public boolean isLet() { return mName != null; }
  public String getName() { return mName; }
  public Expr getCondition() { return mCondition; }
  public Expr getThen() { return mThen; }
  public Expr getElse() { return mElse; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    if (isLet()) {
      builder.append("let ").append(mName).append(" = ");
    } else {
      builder.append("if ");
    }
    builder.append(mCondition);
    builder.append("then\n").append(indent);
    mThen.toString(builder, indent + "    ");
    builder.append("else\n").append(indent);
    mElse.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }

  private final String mName;
  private final Expr mCondition;
  private final Expr mThen;
  private final Expr mElse;
}
