package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * A variable declaration.
 */
public class VarExpr extends Expr {
  VarExpr(Position position, boolean isMutable, Pattern pattern, Expr value) {
    super(position);
    mIsMutable = isMutable;
    mPattern = pattern;
    mValue = value;
  }
  
  public boolean isMutable() { return mIsMutable; }
  public Pattern getPattern() { return mPattern; }
  public Expr getValue() { return mValue; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    if (mIsMutable) {
      builder.append("var ");
    } else {
      builder.append("val ");
    }
    
    builder.append(mPattern).append(" = ");
    mValue.toString(builder, indent);
  }

  private final boolean mIsMutable;
  private final Pattern mPattern;
  private final Expr mValue;
}
