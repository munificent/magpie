package com.stuffwithstuff.magpie.ast;

import java.util.*;

import com.stuffwithstuff.magpie.parser.Position;

public class ArrayExpr extends Expr {
  public ArrayExpr(Position position, List<Expr> elements) {
    super(position);
    mElements = elements;
  }
  
  public List<Expr> getElements() { return mElements; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    
    builder.append("[");
    
    for (int i = 0; i < mElements.size(); i++) {
      builder.append(mElements.get(i));
      if (i < mElements.size() - 1) builder.append(", ");
    }
    
    builder.append("]");
    
    return builder.toString();
  }

  private final List<Expr> mElements;
}
