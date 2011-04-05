package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.util.NotImplementedException;

/**
 * Converts an Expr to a Pattern with (more or less) the same structure. Used
 * to get the left-hand side of an assignment expression and convert it to a
 * pattern after it's already been parsed as an expression.
 */
public class ConvertAssignmentExpr implements ExprVisitor<Expr, Expr> {

  public static Expr convert(Expr target, Expr value) {
    ConvertAssignmentExpr converter = new ConvertAssignmentExpr();
    return target.accept(converter, value);
  }
  
  @Override
  public Expr visit(AssignExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(BlockExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(BoolExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(BreakExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(ClassExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(FnExpr expr, Expr value) {
    return invalidExpression(expr);
  }
  
  @Override
  public Expr visit(IntExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(ListExpr expr, Expr value) {
    throw new NotImplementedException("Destructuring is only implemented on new vars for now.");
  }

  @Override
  public Expr visit(LoopExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(MatchExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(MessageExpr expr, Expr value) {
    if (expr.getReceiver() == null) {
      if (expr.getArg() == null) {
        // message = value  -->  message = value
        return Expr.assign(expr.getPosition(),
            expr.getName(), value);
      } else {
        // message(arg) = value  -->  message_=(arg, value)
        return Expr.message(expr.getPosition(),
            null, Name.makeAssigner(expr.getName()),
            Expr.tuple(expr.getArg(), value));
      }
    } else {
      if (expr.getArg() == null) {
        // receiver message = value  -->  receiver message_=(value)
        return Expr.message(expr.getPosition(),
            expr.getReceiver(), Name.makeAssigner(expr.getName()), value);
      } else {
        // receiver message(arg) = value  -->  receiver message_=(arg, value)
        return Expr.message(expr.getPosition(),
            expr.getReceiver(), Name.makeAssigner(expr.getName()),
            Expr.tuple(expr.getArg(), value));
      }
    }
  }

  @Override
  public Expr visit(MethodExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(NothingExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(RecordExpr expr, Expr value) {
    throw new NotImplementedException("Destructuring is only implemented on new vars for now.");
  }

  @Override
  public Expr visit(ReturnExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(ScopeExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(StringExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(TupleExpr expr, Expr value) {
    throw new NotImplementedException("Destructuring is only implemented on new vars for now.");
  }

  @Override
  public Expr visit(VariableExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  private Expr invalidExpression(Expr expr) {
    throw new ParseException("Expression \"" + expr +
        "\" is not a valid target for assignment.");
  }
}
