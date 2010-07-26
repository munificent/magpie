package com.stuffwithstuff.magpie.ast;

public class IntExpr extends Expr {
  public IntExpr(int value) {
    mValue = value;
  }
  
  public int getValue() { return mValue; }
  
  @Override public String toString() { return Integer.toString(mValue); }

  private final int mValue;
}
