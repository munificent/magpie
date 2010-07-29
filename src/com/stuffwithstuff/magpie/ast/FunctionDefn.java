package com.stuffwithstuff.magpie.ast;

/**
 * AST node for a function definition.
 */
public class FunctionDefn {
  public FunctionDefn(String name, Expr body) {
    mName = name;
    mBody = body;
  }
  
  public String getName() { return mName; }
  public Expr   getBody() { return mBody; }
  
  private final String mName;
  private final Expr   mBody;
}
