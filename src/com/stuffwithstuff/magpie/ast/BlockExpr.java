package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class BlockExpr extends Expr {
  public BlockExpr(Position position, List<Expr> expressions, Expr catchExpr) {
    super(position);
    
    mExpressions = expressions;
    mCatchExpr = catchExpr;
  }
  
  public BlockExpr(Position position, List<Expr> expressions) {
    this(position, expressions, null);
  }
  
  public List<Expr> getExpressions() { return mExpressions; }
  public Expr getCatch() { return mCatchExpr; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("\n");
    indent = indent + "    ";
    for (Expr expr : mExpressions) {
      builder.append(indent);
      expr.toString(builder, indent);
      builder.append("\n");
    }
  }

  private final List<Expr> mExpressions;
  private final Expr mCatchExpr;
}
