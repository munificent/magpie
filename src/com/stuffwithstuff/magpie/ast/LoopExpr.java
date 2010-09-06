package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for loop expressions. Represents a multi-condition "while" loop but
 * also handles "for" loops since the parser desugars those to this.
 */
public class LoopExpr extends Expr {
  public LoopExpr(Position position, List<Expr> conditions, Expr body) {
    super(position);
    
    mConditions = conditions;
    mBody = body;
  }
  
  public List<Expr> getConditions() { return mConditions; }
  public Expr getBody() { return mBody; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    for (int i = 0; i < mConditions.size(); i++) {
      Expr condition = mConditions.get(i);
      builder.append("while ");
      condition.toString(builder, indent);
      
      if (i < mConditions.size() - 1) {
        builder.append("\n").append(indent);
      } else {
        builder.append(" do\n").append(indent);
      }
    }
    mBody.toString(builder, indent + "    ");
    builder.append("\n").append(indent).append("end");
  }
  
  private final List<Expr> mConditions;
  private final Expr mBody;
}
