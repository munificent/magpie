package com.stuffwithstuff.magpie.ast;

import java.util.*;
import com.stuffwithstuff.magpie.type.*;

/**
 * AST node for a class definition expression.
 */
public class ClassExpr extends Expr {
  public ClassExpr(String name, Map<String, TypeDecl> fields) {
    mName = name;
    mFields = fields;
  }
  
  public String getName() { return mName; }
  public Map<String, TypeDecl> getFields() { return mFields; }
  
  @Override
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }
  
  private final String mName;
  private final Map<String, TypeDecl> mFields;
}
