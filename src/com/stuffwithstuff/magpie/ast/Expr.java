package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
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
  
  public static BoolExpr bool(boolean value) {
    return new BoolExpr(Position.none(), value);
  }
  
  public static Expr block(List<Expr> exprs) {
    return block(exprs, null);
  }
  
  public static Expr block(List<Expr> exprs, Expr catchExpr) {
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
  
  public static FnExpr fn(Expr body) {
    return new FnExpr(Position.none(), FunctionType.nothingToDynamic(), body);
  }
  
  public static IntExpr integer(int value) {
    return new IntExpr(Position.none(), value);
  }
  
  public static Expr message(Position position, Expr receiver, String name, Expr arg) {
    return apply(new MessageExpr(position, receiver, name), arg, false);
  }
  
  public static Expr message(Expr receiver, String name, Expr arg) {
    return apply(new MessageExpr(Position.none(), receiver, name), arg, false);
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
    return apply(new MessageExpr(Position.none(), receiver, name), arg, true);
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

