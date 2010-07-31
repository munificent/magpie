package com.stuffwithstuff.magpie.ast;

import java.util.*;
import com.stuffwithstuff.magpie.type.*;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(String name, Map<String, TypeDecl> fields,
      Map<String, FnExpr> methods) {
    mName = name;
    mFields = fields;
    mMethods = methods;
  }
  
  public String getName() { return mName; }
  public Map<String, TypeDecl> getFields() { return mFields; }
  public Map<String, FnExpr> getMethods() { return mMethods; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  private final String mName;
  private final Map<String, TypeDecl> mFields;
  private final Map<String, FnExpr> mMethods;
}
