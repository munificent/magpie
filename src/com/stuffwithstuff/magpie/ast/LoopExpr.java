package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for loop expressions. Simply loops forever. Relies on "break"
 * expressions in the body to exit the loop.
 */
public class LoopExpr extends Expr {
  LoopExpr(Position position, Expr body) {
    super(position);
    
    mBody = body;
  }
  
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append("loop\n");
    mBody.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }
  
  private final Expr mBody;
}
