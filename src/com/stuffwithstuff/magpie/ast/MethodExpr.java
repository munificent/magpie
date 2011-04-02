package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;

public class MethodExpr extends Expr {
  MethodExpr(Position position, String name, Pattern pattern, Expr body) {
    super(position);
    mName = name;
    mPattern = pattern;
    mBody = body;
  }
  
  public String getName() { return mName; }
  public Pattern getPattern() { return mPattern; }
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("not impl");
  }

  private final String mName;
  private final Pattern mPattern;
  private final Expr mBody;
}
