package com.stuffwithstuff.magpie.ast;

import java.util.List;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(String name, List<Expr> body) {
    mName = name;
    mBody = body;
  }
  
  public String getName() { return mName; }
  public List<Expr> getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  private final String mName;
  private final List<Expr> mBody;
}
