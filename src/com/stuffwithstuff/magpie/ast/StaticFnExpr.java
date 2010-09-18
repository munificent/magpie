package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node class for a static function definition. A static function is a
 * lambda that is evaluated both at runtime and at type-check type (as opposed
 * to regular lambdas which are checked at check time).
 */
public class StaticFnExpr extends Expr {
  public StaticFnExpr(Position position, List<String> params, Expr body) {
    super(position);
    
    mParams = params;
    mBody = body;
  }
  
  public List<String> getParams()  { return mParams; }
  public Expr         getBody()  { return mBody; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }
  
  @Override
  public void toString(StringBuilder builder, String indent) {
    // TODO(bob): This syntax is completely temporary!
    builder.append("{ ");
    
    for(String name : mParams) {
      builder.append(name).append(" ");
    }
    
    builder.append(":\n");
    mBody.toString(builder, indent + "  ");
    builder.append(indent).append("\n}");
  }

  private final List<String>  mParams;
  private final Expr          mBody;
}
