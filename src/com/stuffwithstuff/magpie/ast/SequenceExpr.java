package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class SequenceExpr extends Expr {
  SequenceExpr(List<Expr> expressions) {
    super(Position.surrounding(expressions));
    
    mExpressions = expressions;
  }
  
  public List<Expr> getExpressions() { return mExpressions; }
  
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
}
