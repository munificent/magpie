package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for loop expressions. Represents a multi-condition "while" loop but
 * also handles "for" loops since the parser desugars those to this.
 */
public class LoopExpr extends Expr {
  public LoopExpr(Position position, List<Expr> conditions, Expr body) {
    super(position);
    
    mConditions = conditions;
    mBody = body;
  }
  
  public List<Expr> getConditions() { return mConditions; }
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  private final List<Expr> mConditions;
  private final Expr mBody;
}
