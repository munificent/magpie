package com.stuffwithstuff.magpie.ast;

/**
 * Represents a single "if" or "let" condition in a conditional expression.
 * Unlike most languages, Magpie allows multiple conditions in a single
 * expression, like:
 * 
 * if a < b
 * if c > d
 * let e = foo
 * then
 *     doStuff
 * end
 * 
 * This class defines one clause that appears before the "then".
 */
public class Condition {
  public Condition(String name, Expr body) {
    mName = name;
    mBody = body;
  }
  
  public Condition(Expr body) {
    this (null, body);
  }
  
  public String getName() { return mName; }
  public boolean isLet() { return mName != null; }
  public Expr getBody() { return mBody; }
  
  @Override
  public String toString() {
    if (isLet()) {
      return "let " + mName + " = " + mBody;
    } else {
      return "if " + mBody;
    }
  }
  private final String mName;
  private final Expr mBody;
}
