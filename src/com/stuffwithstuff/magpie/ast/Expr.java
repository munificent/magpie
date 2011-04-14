package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Base class for AST expression node classes. Any chunk of Magpie code can be
 * represented by an instance of one of the subclasses of this class.
 * 
 * <p>Also includes factory methods to create the appropriate nodes.
 * 
 * @author bob
 */
public abstract class Expr {
  public static Expr assign(Position position, String name, Expr value) {
    return new AssignExpr(position, name, value);
  }
  
  public static Expr bool(boolean value) {
    return bool(Position.none(), value);
  }
  
  public static Expr bool(Position position, boolean value) {
    return new BoolExpr(position, value);
  }
  
  public static Expr block(List<Expr> exprs) {
    return block(exprs, null);
  }
  
  public static Expr block(List<Expr> exprs, List<MatchCase> catches) {
    // Discard unneeded blocks.
    if ((catches == null) || (catches.size() == 0)) {
      switch (exprs.size()) {
      case 0:
        return nothing();
      case 1:
        return exprs.get(0);
      default:
        return new BlockExpr(exprs, null);
      }
    } else {
      return new BlockExpr(exprs, catches);
    }
  }

  public static Expr block(Expr... exprs) {
    return new BlockExpr(Arrays.asList(exprs), null);
  }

  public static Expr break_(Position position) {
    return new BreakExpr(position);
  }
  
  public static Expr call(Position position, Expr receiver, String name, Expr arg) {
    return new CallExpr(position, receiver, name, arg);
  }
  
  public static Expr call(Position position, Expr receiver, String name) {
    return call(position, receiver, name, null);
  }

  public static Expr class_(Position position,
      String name, List<String> parents, Map<String, Field> fields) {
    return new ClassExpr(position, name, parents, fields);
  }

  public static Expr define(String name, Expr value) {
    return define(value.getPosition(), name, value);
  }

  public static Expr define(Position position, String name, Expr value) {
    return define(position, Pattern.variable(name), value);
  }
  
  public static Expr define(Position position, Pattern pattern, Expr value) {
    return new DefineExpr(position, pattern, value);
  }

  public static FnExpr fn(Expr body) {
    return new FnExpr(Position.none(), body);
  }
  
  public static FnExpr fn(Position position, Expr body) {
    return new FnExpr(position, body);
  }
  
  public static FnExpr fn(Position position, Pattern pattern, Expr body) {
    return new FnExpr(position, pattern, body);
  }
  
  // TODO(bob): Hackish. Eliminate.
  public static Expr if_(Expr condition, Expr thenExpr, Expr elseExpr) {
    if (elseExpr == null) elseExpr = Expr.nothing();
    
    List<MatchCase> cases = new ArrayList<MatchCase>();
    cases.add(new MatchCase(Pattern.value(Expr.bool(true)), thenExpr));
    cases.add(new MatchCase(elseExpr));
    
    return match(Position.surrounding(condition, elseExpr),
        condition,
        cases);
  }
  
  public static Expr import_(Position position, String name) {
    return new ImportExpr(position, name);
  }
  
  public static Expr int_(int value) {
    return int_(Position.none(), value);
  }
  
  public static Expr int_(Position position, int value) {
    return new IntExpr(position, value);
  }
  
  public static Expr list(Position position, List<Expr> elements) {
    return new ListExpr(position, elements);
  }
  
  public static Expr loop(Position position, Expr body) {
    return new LoopExpr(position, body);
  }
  
  public static Expr match(Position position,
      Expr value, List<MatchCase> cases) {
    return new MatchExpr(position, value, cases);
  }
  
  public static Expr method(Position position, String name, Pattern pattern, Expr body) {
    return new MethodExpr(position, name, pattern, body);
  }
  
  public static Expr nothing() {
    return nothing(Position.none());  
  }
  
  public static Expr nothing(Position position) {
    return new NothingExpr(Position.none());  
  }

  public static Expr record(Position position,
      List<Pair<String, Expr>> fields) {
    return new RecordExpr(position, fields);
  }

  public static Expr scope(Expr body) {
    // Unwrap redundant scopes.
    if (body instanceof ScopeExpr) return body;
    return new ScopeExpr(body);
  }

  public static Expr string(String text) {
    return string(Position.none(), text);
  }

  public static Expr string(Position position, String text) {
    return new StringExpr(position, text);
  }
  
  public static Expr tuple(List<Expr> fields) {
    return new TupleExpr(fields);
  }

  public static Expr tuple(Expr... fields) {
    return new TupleExpr(Arrays.asList(fields));
  }
  
  public static Expr variable(Position position, String name) {
    return new VariableExpr(position, name);
  }
  
  public static Expr variable(String name) {
    return variable(Position.none(), name);
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

