package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;

import com.stuffwithstuff.magpie.parser.Position;

public abstract class Expr {
  public static Expr fn(Expr body) {
    return new FnExpr(
        Position.none(),
        new FunctionType(new ArrayList<String>(),
            Expr.name("Nothing"), Expr.name("Dynamic")),
        body);
  }
  
  public static Expr message(Expr receiver, String name, Expr arg) {
    return new MessageExpr(Position.none(), receiver, name, arg);
  }
  
  public static Expr message(Expr receiver, String name) {
    return new MessageExpr(Position.none(), receiver, name, null);
  }
  
  public static Expr name(String name) {
    return new MessageExpr(Position.none(), null, name, null);
  }
  
  public static Expr string(String text) {
    return new StringExpr(Position.none(), text);
  }
  
  public static Expr tuple(Expr... fields) {
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

