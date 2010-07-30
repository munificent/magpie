package com.stuffwithstuff.magpie.ast;

import java.util.List;

/**
 * AST node for loop expressions. Represents a multi-condition "while" loop but
 * also handles "for" loops since the parser desugars those to this.
 */
public class LoopExpr extends Expr {
  public LoopExpr(List<Expr> conditions, Expr body) {
    mConditions = conditions;
    mBody = body;
  }
  
  public List<Expr> getConditions() { return mConditions; }
  public Expr getBody() { return mBody; }
  
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  private final List<Expr> mConditions;
  private final Expr mBody;
}
