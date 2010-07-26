package com.stuffwithstuff.magpie.ast;

public abstract class Expr {
  public abstract <T> T accept(ExprVisitor<T> visitor);
}
