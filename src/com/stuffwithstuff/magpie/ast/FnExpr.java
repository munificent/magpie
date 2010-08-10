package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node class for an function definition.
 */
public class FnExpr extends Expr {
  public FnExpr(Position position, List<String> paramNames,
      Expr paramType, Expr returnType, Expr body) {
    super(position);
    
    mParamNames = paramNames;
    mParamType = paramType;
    mReturnType = returnType;
    mBody = body;
  }
  
  public List<String> getParamNames() { return mParamNames; }
  public Expr getParamType()  { return mParamType; }
  public Expr getReturnType() { return mReturnType; }
  public Expr getBody()       { return mBody; }

  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  private final List<String>  mParamNames;
  private final Expr          mBody;
  private final Expr          mParamType;
  private final Expr          mReturnType;
}
