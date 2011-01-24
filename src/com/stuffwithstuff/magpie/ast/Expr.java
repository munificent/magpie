package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Base class for AST expression node classes. Any chunk of Magpie code can be
 * represented by an instance of one of the subclasses of this class.
 * 
 * <p>Also includes factory methods to create the appropriate nodes. Aside from
 * being a bit more terse, these methods perform some basic desugaring and
 * simplification such as expanding special forms and discarding useless nodes.
 * 
 * @author bob
 */
public abstract class Expr {
  public static Expr and(Expr left, Expr right) {
    return new AndExpr(left, right);
  }
  
  public static Expr assign(Position position, Expr receiver, String name,
      Expr value) {
    return new AssignExpr(position, receiver, name, value);
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
  
  public static Expr block(List<Expr> exprs, Expr catchExpr) {
    // Discard unneeded blocks.
    if (catchExpr == null) {
      switch (exprs.size()) {
      case 0:
        return nothing();
      case 1:
        return exprs.get(0);
      default:
        return new BlockExpr(exprs, null);
      }
    } else {
      return new BlockExpr(exprs, catchExpr);
    }
  }

  public static Expr block(Expr... exprs) {
    return new BlockExpr(Arrays.asList(exprs));
  }

  public static Expr break_(Position position) {
    return new BreakExpr(position);
  }
  
  public static Expr call(Expr target, List<Expr> typeArgs, Expr arg) {
    Expect.notNull(target);
    Expect.notNull(typeArgs);
    Expect.notNull(arg);
    
    return new CallExpr(target, typeArgs, arg);
  }
  
  public static Expr call(Expr target, Expr arg) {
    Expect.notNull(target);
    Expect.notNull(arg);
    
    // Immediately handle special forms.
    if (target instanceof MessageExpr) {
      MessageExpr message = (MessageExpr) target;
      String name = message.getName();
      
      if (name.equals("%break%")) {
        return new BreakExpr(target.getPosition());
      } else if (name.equals("%if%")) {
        return specialFormIf(target.getPosition(), arg);
      } else if (name.equals("%return%")) {
        return new ReturnExpr(target.getPosition(), arg);
      } else if (name.equals("%scope%")) {
        return new ScopeExpr(arg);
      } else if (name.equals("%unsafecast%")) {
        return specialFormUnsafeCast(target.getPosition(), arg);
      } else if (name.equals("%var%")) {
        return specialFormVar(target.getPosition(), arg);
      }
    }
    
    // If we got here, it's not a special form.
    return new CallExpr(target, null, arg);
  }
  
  public static FnExpr fn(Expr body) {
    return fn(Position.none(), FunctionType.nothingToDynamic(), body);
  }
  
  public static FnExpr fn(Position position, FunctionType type, Expr body) {
    return new FnExpr(position, type, body);
  }
  
  public static Expr if_(Expr condition, Expr thenExpr, Expr elseExpr) {
    return new IfExpr(Position.surrounding(condition, elseExpr), null,
        condition, thenExpr, elseExpr);
  }
  
  public static Expr int_(int value) {
    return int_(Position.none(), value);
  }
  
  public static Expr int_(Position position, int value) {
    return new IntExpr(position, value);
  }
  
  public static Expr loop(Position position, Expr body) {
    return new LoopExpr(position, body);
  }
  
  public static Expr match(Position position,
      Expr value, List<MatchCase> cases) {
    return new MatchExpr(position, value, cases);
  }
  
  public static Expr message(Position position, Expr receiver, String name, Expr arg) {
    return call(message(position, receiver, name), arg);
  }
  
  public static Expr message(Expr receiver, String name, Expr arg) {
    return call(message(receiver, name), arg);
  }
  
  public static Expr message(Expr receiver, String name) {
    return message(Position.none(), receiver, name);
  }
  
  public static Expr message(Position position, Expr receiver, String name) {
    return new MessageExpr(position, receiver, name);
  }
  
  public static Expr name(Position position, String name) {
    return new MessageExpr(position, null, name);
  }
  
  public static Expr name(String name) {
    return new MessageExpr(Position.none(), null, name);
  }
  
  public static Expr nothing() {
    return nothing(Position.none());  
  }
  
  public static Expr nothing(Position position) {
    return new NothingExpr(Position.none());  
  }

  public static Expr or(Expr left, Expr right) {
    return new OrExpr(left, right);
  }
  
  public static Expr quote(Position position, Expr body) {
    return new QuotationExpr(position, body);
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

  public static Expr staticMessage(Expr receiver, String name, Expr arg) {
    return call(new MessageExpr(Position.none(), receiver, name), 
        Collections.singletonList(arg), nothing());
  }

  public static Expr string(String text) {
    return string(Position.none(), text);
  }

  public static Expr string(Position position, String text) {
    return new StringExpr(position, text);
  }
  
  public static Expr this_(Position position) {
    return new ThisExpr(position);  
  }

  public static Expr tuple(List<Expr> fields) {
    return new TupleExpr(fields);
  }

  public static Expr tuple(Expr... fields) {
    return new TupleExpr(Arrays.asList(fields));
  }

  public static Expr var(String name, Expr value) {
    return var(value.getPosition(), name, value);
  }

  public static Expr var(Position position, String name, Expr value) {
    return var(position, new VariablePattern(name, null), value);
  }
  
  public static Expr var(Position position, Pattern pattern, Expr value) {
    return new VariableExpr(position, pattern, value);
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
  
  private static Expr specialFormIf(Position position, Expr arg) {
    List<Expr> args = splitArg(arg);
    
    if ((args.size() < 2) || (args.size() > 3)) {
      throw new IllegalArgumentException(
      "The %if% special form requires 2 or 3 arguments.");
    }
    
    Expr condition = args.get(0);
    Expr thenExpr = args.get(1);

    Expr elseExpr;
    if (args.size() == 3) {
      elseExpr = args.get(2);
    } else {
      elseExpr = Expr.nothing();
    }
    
    return new IfExpr(position, null, condition, thenExpr, elseExpr);
  }

  private static Expr specialFormUnsafeCast(Position position, Expr arg) {
    Expr type = ((TupleExpr)arg).getFields().get(0);
    Expr value = ((TupleExpr)arg).getFields().get(1);
    
    return new UnsafeCastExpr(position, type, value);
  }
  
  private static Expr specialFormVar(Position position, Expr arg) {
    String name = ((StringExpr)(((TupleExpr)arg).getFields().get(0))).getValue();
    Expr valueExpr = ((TupleExpr)arg).getFields().get(1);
    
    return var(position, name, valueExpr);
  }
  
  private static List<Expr> splitArg(Expr arg) {
    List<Expr> args;
    if (arg instanceof TupleExpr) {
      args = ((TupleExpr)arg).getFields();
    } else {
      args = new ArrayList<Expr>();
      args.add(arg);
    }
    
    return args;
  }
  
  private final Position mPosition;
}

