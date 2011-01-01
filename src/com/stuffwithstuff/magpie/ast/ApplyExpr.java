package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for a function application: applies an argument to a function-like
 * target.
 */
public class ApplyExpr extends Expr {
  public static Expr create(Expr target, Expr arg, boolean isStatic) {
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
  
  private ApplyExpr(Expr target, Expr arg, boolean isStatic) {
    super(target.getPosition().union(arg.getPosition()));
    mTarget = target;
    mArg = arg;
    mIsStatic = isStatic;
  }
  
  public Expr getTarget() { return mTarget; }
  public Expr getArg() { return mArg; }
  public boolean isStatic() { return mIsStatic; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    mTarget.toString(builder, indent);
    builder.append(mIsStatic ? "[" : "(");
    mArg.toString(builder, indent);
    builder.append(mIsStatic ? "]" : ")");
  }

  private final Expr mTarget;
  private final Expr mArg;
  private final boolean mIsStatic;
}
