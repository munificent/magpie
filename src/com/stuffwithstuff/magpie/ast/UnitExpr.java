package com.stuffwithstuff.magpie.ast;

public class UnitExpr extends Expr {
  public UnitExpr() {
  }
  
  @Override public String toString() { return "()"; }
}
