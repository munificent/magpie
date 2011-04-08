package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * A variable declaration.
 */
public class DefineExpr extends Expr {
  DefineExpr(Position position, Pattern pattern, Expr value) {
    super(position);
    mPattern = pattern;
    mValue = value;
  }
  
  public Pattern getPattern() { return mPattern; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("var ").append(mPattern).append(" = ");
    mValue.toString(builder, indent);
  }

  private final Pattern mPattern;
  private final Expr mValue;
}
