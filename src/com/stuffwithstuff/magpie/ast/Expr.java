package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public abstract class Expr {
  public static BoolExpr bool(boolean value) {
    return new BoolExpr(Position.none(), false);
  }
  
  public static FnExpr fn(Expr body) {
    return new FnExpr(Position.none(), FunctionType.nothingToDynamic(), body, false);
  }
  
  public static ApplyExpr message(Position position, Expr receiver, String name, Expr arg) {
    return new ApplyExpr(new MessageExpr(position, receiver, name), arg, false);
  }
  
  public static ApplyExpr message(Expr receiver, String name, Expr arg) {
    return new ApplyExpr(new MessageExpr(Position.none(), receiver, name), arg, false);
  }
  
  public static MessageExpr message(Expr receiver, String name) {
    return new MessageExpr(Position.none(), receiver, name);
  }
  
  public static MessageExpr name(String name) {
    return new MessageExpr(Position.none(), null, name);
  }
  
  public static NothingExpr nothing() {
    return new NothingExpr(Position.none());  
  }
  
  public static StringExpr string(String text) {
    return new StringExpr(Position.none(), text);
  }
  
  public static TupleExpr tuple(Expr... fields) {
    return new TupleExpr(fields);
  }
  
  public Expr(Position position) {
    mPosition = position;
  }
  
  public Position getPosition() { return mPosition; }
  
  public abstract <TReturn, TContext> TReturn accept(
      ExprVisitor<TReturn, TContext> visitor, TContext context);
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, "");
    return builder.toString();
  }
  
  public abstract void toString(StringBuilder builder, String indent);
  
  private final Position mPosition;
}

