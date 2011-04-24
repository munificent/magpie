package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * AST node class for an function definition.
 */
public class FnExpr extends Expr {
  FnExpr(Position position, String doc, Expr body) {
    this(position, doc, Pattern.nothing(), body);
  }
  
  FnExpr(Position position, String doc, Pattern pattern, Expr body) {
    super(position, doc);
    Expect.notNull(pattern);
    mPattern = pattern;
    mBody = body;
  }
  
  public Pattern getPattern() { return mPattern; }
  public Expr    getBody()    { return mBody; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("fn(").append(mPattern).append(") ");
    mBody.toString(builder, indent);
  }

  private final Pattern  mPattern;
  private final Expr     mBody;
}
