package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.type.FunctionType;

/**
 * AST node class for an function definition.
 */
public class FnExpr extends Expr {
  public FnExpr(FunctionType type, List<String> paramNames, Expr body) {
    mType = type;
    mParamNames = paramNames;
    mBody = body;
  }
  
  public FunctionType getType() { return mType; }
  public List<String> getParamNames() { return mParamNames; }
  public Expr   getBody() { return mBody; }

  @Override
  public <T> T accept(ExprVisitor<T> visitor) { return visitor.visit(this); }

  private final FunctionType mType;
  private final List<String> mParamNames;
  private final Expr   mBody;
}
