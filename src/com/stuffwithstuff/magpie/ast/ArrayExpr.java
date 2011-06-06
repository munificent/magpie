package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class ArrayExpr extends Expr {
  ArrayExpr(Position position, List<Expr> elements) {
    super(position);
    
    mElements = elements;
  }
  
  public List<Expr> getElements() { return mElements; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("[");
    for (int i = 0; i < mElements.size(); i++) {
      mElements.get(i).toString(builder, indent);
      if (i < mElements.size() - 1) builder.append(", ");
    }
    builder.append("]");
  }

  private final List<Expr> mElements;
}
