package com.stuffwithstuff.magpie.ast;

import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.parser.Position;

public class ClassExpr extends Expr {
  ClassExpr(Position position, String doc, String name, List<String> parents,
      Map<String, Field> fields) {
    super(position, doc);
    mName = name;
    mParents = parents;
    mFields = fields;
  }
  
  public String getName() { return mName; }
  public List<String> getParents() { return mParents; }
  public Map<String, Field> getFields() { return mFields; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("not impl");
  }

  private final String mName;
  private final List<String> mParents;
  private final Map<String, Field> mFields;
}
