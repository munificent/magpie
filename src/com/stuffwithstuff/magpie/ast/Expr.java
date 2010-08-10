package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;


public abstract class Expr {
  public Expr(Position position) {
    mPosition = position;
  }
  
  public Position getPosition() { return mPosition; }
  
  public abstract <TReturn, TContext> TReturn accept(
      ExprVisitor<TReturn, TContext> visitor, TContext context);
  
  private Position mPosition;
}

