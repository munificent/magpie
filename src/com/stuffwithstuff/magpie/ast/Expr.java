package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;

import com.stuffwithstuff.magpie.parser.Position;

public abstract class Expr {
  public static FnExpr fn(Expr body) {
    return new FnExpr(
        Position.none(),
        new FunctionType(new ArrayList<String>(),
            Expr.name("Nothing"), Expr.name("Dynamic")),
        body);
  }
  
  public static MessageExpr message(Expr receiver, String name, Expr arg) {
    return new MessageExpr(Position.none(), receiver, name, arg);
  }
  
  public static MessageExpr message(Expr receiver, String name) {
    return new MessageExpr(Position.none(), receiver, name, null);
  }
  
  public static MessageExpr name(String name) {
    return new MessageExpr(Position.none(), null, name, null);
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
  
  private Position mPosition;
}

