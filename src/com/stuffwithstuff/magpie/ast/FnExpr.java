package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.type.FunctionType;

/**
 * AST node class for an function definition.
 */
public class FnExpr extends Expr {
  public FnExpr(Position position, FunctionType type, List<String> paramNames, Expr body) {
    super(position);
    
    mType = type;
    mParamNames = paramNames;
    mBody = body;
  }
  
  public FunctionType getType() { return mType; }
  public List<String> getParamNames() { return mParamNames; }
  public Expr   getBody() { return mBody; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  private final FunctionType mType;
  private final List<String> mParamNames;
  private final Expr   mBody;
}
