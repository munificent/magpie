package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.parser.Position;

public class MatchExpr extends Expr {
  MatchExpr(Position position, Expr value, List<MatchCase> cases) {
    super(position);
    
    mValue = value;
    mCases = cases;
  }
  
  public Expr            getValue() { return mValue; }
  public List<MatchCase> getCases() { return mCases; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    builder.append(indent).append("match ");
    mValue.toString(builder, indent);
    builder.append("\n");
    
    for (MatchCase matchCase : mCases) {
      builder.append(indent).append("    case ");
      builder.append(matchCase.getPattern());
      builder.append(" then ");
      matchCase.getBody().toString(builder, indent + "        ");
      builder.append("\n");
    }
    
    builder.append(indent).append("end");
  }

  private final Expr mValue;
  private final List<MatchCase> mCases;
}
