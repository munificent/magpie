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
  public Expr visit(BoolExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(BreakExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(CallExpr expr, Expr value) {
    if (expr.getReceiver() == null) {
      // message(arg) = value  -->  message_=(arg, value)
      return Expr.call(expr.getPosition(),
          null, Name.makeAssigner(expr.getName()),
          Expr.record(expr.getArg(), value));
    } else {
      if (expr.getArg() == null) {
        // receiver message = value  -->  receiver message_=(value)
        return Expr.call(expr.getPosition(),
            expr.getReceiver(), Name.makeAssigner(expr.getName()), value);
      } else {
        // receiver message(arg) = value  -->  receiver message_=(arg, value)
        return Expr.call(expr.getPosition(),
            expr.getReceiver(), Name.makeAssigner(expr.getName()),
            Expr.record(expr.getArg(), value));
      }
    }
  }
  
  @Override
  public Expr visit(ClassExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(DefineExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(FnExpr expr, Expr value) {
    return invalidExpression(expr);
  }
  
  @Override
  public Expr visit(ImportExpr expr, Expr value) {
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
  public Expr visit(MethodExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(NothingExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(RecordExpr expr, Expr value) {
    // a, b = 1, 2
    // becomes:
    // match 1, 2
    //     case temp1, temp2
    //         a = temp1
    //         b = temp2
    //     end
    // end
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
  public Expr visit(SequenceExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(StringExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(ThrowExpr expr, Expr value) {
    return invalidExpression(expr);
  }

  @Override
  public Expr visit(VariableExpr expr, Expr value) {
    // message = value  -->  message = value
    return Expr.assign(expr.getPosition(), expr.getName(), value);
  }

  private Expr invalidExpression(Expr expr) {
    throw new ParseException("Expression \"" + expr +
        "\" is not a valid target for assignment.");
  }
}
