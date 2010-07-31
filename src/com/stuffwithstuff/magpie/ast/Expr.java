package com.stuffwithstuff.magpie.ast;

public abstract class Expr {
  public abstract <TReturn, TContext> TReturn accept(
      ExprVisitor<TReturn, TContext> visitor, TContext context);
}

