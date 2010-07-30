package com.stuffwithstuff.magpie.ast;

import java.util.Set;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(String name, Set<String> fields) {
    mName = name;
    mFields = fields;
  }
  
  public String getName() { return mName; }
  public Set<String> getFields() { return mFields; }
  
  @Override
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }
  
  private final String mName;
  private final Set<String> mFields;
}
