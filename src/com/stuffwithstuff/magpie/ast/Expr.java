package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

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
  
  public static Expr apply(Expr target, Expr arg, boolean isStatic) {
    // Immediately handle special forms.
    if (target instanceof MessageExpr) {
      MessageExpr message = (MessageExpr) target;
      String name = message.getName();
      
      if (name.equals("%break%")) {
        return new BreakExpr(target.getPosition());
      } else if (name.equals("%fn%")) {
        return specialFormFn(target.getPosition(), arg);
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
    return new ApplyExpr(target, arg, isStatic);
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
  
  public static Expr message(Position position, Expr receiver, String name, Expr arg) {
    return apply(message(position, receiver, name), arg, false);
  }
  
  public static Expr message(Expr receiver, String name, Expr arg) {
    return apply(message(receiver, name), arg, false);
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

  public static Expr scope(Expr body) {
    // Unwrap redundant scopes.
    if (body instanceof ScopeExpr) return body;
    return new ScopeExpr(body);
  }

  public static Expr staticMessage(Expr receiver, String name, Expr arg) {
    return apply(new MessageExpr(Position.none(), receiver, name), arg, true);
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
    return new VariableExpr(position, name, value);
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
  
  private static Expr specialFormFn(Position position, Expr arg) {
    List<Expr> args = splitArg(arg);
    
    // This special form allows omitting certain arguments, so the
    // interpretation of it is based on how many are provided.
    List<String> paramNames = new ArrayList<String>();
    Expr paramType;
    Expr returnType;
    boolean hasNames = false;
    boolean isStatic = false;

    switch (args.size()) {
    case 1: // fn(body) Nothing -> Dynamic
      paramType = Expr.name("Nothing");
      returnType = Expr.name("Dynamic");
      break;
      
    case 2: // fn(returnType, body) Nothing -> returnType
      paramType = Expr.name("Nothing");
      returnType = args.get(0);
      break;
      
    case 3: // fn(names, returnType, body) Dynamic -> returnType
      hasNames = true;
      paramType = Expr.name("Dynamic");
      returnType = args.get(0);
      break;
      
    case 4: // fn(names, paramType, returnType, body)
      hasNames = true;
      paramType = args.get(1);
      returnType = args.get(2);
      break;
      
    case 5: // fn(names, paramType, returnType, isStatic, body)
      hasNames = true;
      paramType = args.get(1);
      returnType = args.get(2);
      isStatic = ((BoolExpr)args.get(3)).getValue();
      break;
      
    default:
        throw new IllegalArgumentException(
            "The %fn% special form requires 1 to 5 arguments.");
    }
    
    if (hasNames) {
      if (args.get(0) instanceof TupleExpr) {
        List<Expr> paramNameExprs = ((TupleExpr)args.get(0)).getFields();
        for (Expr paramName : paramNameExprs) {
          paramNames.add(((StringExpr)paramName).getValue());
        }
      } else {
        // Just a single name.
        paramNames.add(((StringExpr)args.get(0)).getValue());
      }
    }
    
    // Body is always the last argument.
    Expr body = args.get(args.size() - 1);
    
    FunctionType type = new FunctionType(paramNames, paramType, returnType,
        isStatic);
    return new FnExpr(position, type, body);
  }

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
    
    return new VariableExpr(position, name, valueExpr);
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

