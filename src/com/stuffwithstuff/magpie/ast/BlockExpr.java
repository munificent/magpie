package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.parser.Position;

// TODO(bob): The "catch" stuff really shouldn't be here. It should be its own
// node or, better, be part of ScopeExpr.
public class BlockExpr extends Expr {
  BlockExpr(List<Expr> expressions, List<MatchCase> catches) {
    super(Position.surrounding(expressions));
    
    mExpressions = expressions;
    mCatches = catches;
  }
  
  public List<Expr> getExpressions() { return mExpressions; }
  public List<MatchCase> getCatches() { return mCatches; }
  
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
    // TODO(bob): Catches.
  }

  private final List<Expr> mExpressions;
  private final List<MatchCase> mCatches;
}
