package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

public abstract class Expr {
  public static BoolExpr bool(boolean value) {
    return new BoolExpr(Position.none(), value);
  }
  
  public static FnExpr fn(Expr body) {
    return new FnExpr(Position.none(), FunctionType.nothingToDynamic(), body);
  }
  
  public static IntExpr integer(int value) {
    return new IntExpr(Position.none(), value);
  }
  
  public static Expr message(Position position, Expr receiver, String name, Expr arg) {
    return ApplyExpr.create(new MessageExpr(position, receiver, name), arg, false);
  }
  
  public static Expr message(Expr receiver, String name, Expr arg) {
    return ApplyExpr.create(new MessageExpr(Position.none(), receiver, name), arg, false);
  }
  
  public static MessageExpr message(Expr receiver, String name) {
    return new MessageExpr(Position.none(), receiver, name);
  }
  
  public static MessageExpr name(Position position, String name) {
    return new MessageExpr(position, null, name);
  }
  
  public static MessageExpr name(String name) {
    return new MessageExpr(Position.none(), null, name);
  }
  
  public static NothingExpr nothing() {
    return new NothingExpr(Position.none());  
  }
  
  public static Expr staticMessage(Expr receiver, String name, Expr arg) {
    return ApplyExpr.create(new MessageExpr(Position.none(), receiver, name), arg, true);
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

