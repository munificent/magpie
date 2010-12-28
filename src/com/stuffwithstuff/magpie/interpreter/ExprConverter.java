package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Takes an expression in Java form and creates a first-class Magpie Expr object
 * out of it for interacting with code at runtime.
 */
public class ExprConverter implements ExprVisitor<Obj, Void> {
  public static Obj convert(Interpreter interpreter, Expr expr,
      EvalContext context) {
    ExprConverter converter = new ExprConverter(interpreter, context);
    return converter.convert(expr);
  }
  
  /**
   * Converts a Magpie expression object into a corresponding Java Expr.
   */
  public static Expr convert(Interpreter interpreter, Obj expressionObj) {
    ClassObj exprClass = expressionObj.getClassObj();
    // TODO(bob): Fill in other expression types.
    // TODO(bob): Support position information in Magpie parser.
    if (exprClass == interpreter.getGlobal("BreakExpression")) {
      return new BreakExpr(Position.none());
    }
    
    // TODO(bob): Add better error-handling.
    return null;
  }
  
  @Override
  public Obj visit(AndExpr expr, Void dummy) {
    Obj left  = convert(expr.getLeft());
    Obj right = convert(expr.getRight());
    return construct("And", left, right);
  }

  @Override
  public Obj visit(ApplyExpr expr, Void dummy) {
    Obj target = convert(expr.getTarget());
    Obj arg = convert(expr.getArg());
    return construct("Apply", target, arg);
  }
  
  @Override
  public Obj visit(AssignExpr expr, Void dummy) {
    Obj receiver = convert(expr.getReceiver());
    Obj value = convert(expr.getValue());
    return construct("Assign",
        receiver, mInterpreter.createString(expr.getName()), value);
  }

  @Override
  public Obj visit(BlockExpr expr, Void dummy) {
    List<Obj> exprs = new ArrayList<Obj>();
    for (Expr blockExpr : expr.getExpressions()) {
      exprs.add(convert(blockExpr));
    }
    Obj exprsArray = mInterpreter.createArray(exprs);
    
    return construct("Block", exprsArray);
  }

  @Override
  public Obj visit(BoolExpr expr, Void dummy) {
    return construct("Bool", mInterpreter.createBool(expr.getValue()));
  }

  @Override
  public Obj visit(BreakExpr expr, Void dummy) {
    return construct("Break");
  }

  @Override
  public Obj visit(ExpressionExpr expr, Void dummy) {
    return construct("Expression", convert(expr.getBody()));
  }

  @Override
  public Obj visit(FnExpr expr, Void dummy) {
    Obj paramType = convert(expr.getType().getParamType());
    Obj returnType = convert(expr.getType().getReturnType());
    List<Obj> paramNames = new ArrayList<Obj>();
    for (String name : expr.getType().getParamNames()) {
      paramNames.add(mInterpreter.createString(name));
    }
    Obj type = construct("FunctionType", mInterpreter.createArray(paramNames),
        paramType, returnType,
        mInterpreter.createBool(expr.getType().isStatic()));
    Obj body = convert(expr.getBody());
    return construct("Function", type, body);
  }

  @Override
  public Obj visit(IfExpr expr, Void dummy) {
    Obj name = (expr.getName() != null) ?
        mInterpreter.createString(expr.getName()) : mInterpreter.nothing();
    Obj condition = convert(expr.getCondition());
    Obj thenArm = convert(expr.getThen());
    Obj elseArm = convert(expr.getElse());
    return construct("If", name, condition, thenArm, elseArm);
  }

  @Override
  public Obj visit(IntExpr expr, Void dummy) {
    return construct("Int", mInterpreter.createInt(expr.getValue()));
  }

  @Override
  public Obj visit(LoopExpr expr, Void dummy) {
    List<Obj> conditions = new ArrayList<Obj>();
    for (Expr condition : expr.getConditions()) {
      conditions.add(convert(condition));
    }
    Obj body = convert(expr.getBody());
    
    return construct("Loop", mInterpreter.createArray(conditions), body);
  }

  @Override
  public Obj visit(MessageExpr expr, Void dummy) {
    Obj receiver = convert(expr.getReceiver());
    return construct("Message", receiver,
        mInterpreter.createString(expr.getName()));
  }

  @Override
  public Obj visit(NothingExpr expr, Void dummy) {
    return construct("Nothing");
  }

  @Override
  public Obj visit(OrExpr expr, Void dummy) {
    Obj left  = convert(expr.getLeft());
    Obj right = convert(expr.getRight());
    return construct("Or", left, right);
  }

  @Override
  public Obj visit(RecordExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(ReturnExpr expr, Void dummy) {
    return construct("Return", convert(expr.getValue()));
  }

  @Override
  public Obj visit(ScopeExpr expr, Void dummy) {
    return construct("Scope", convert(expr.getBody()));
  }

  @Override
  public Obj visit(StringExpr expr, Void dummy) {
    return construct("String", mInterpreter.createString(expr.getValue()));
  }

  @Override
  public Obj visit(ThisExpr expr, Void dummy) {
    return construct("This");
  }

  @Override
  public Obj visit(TupleExpr expr, Void dummy) {
    List<Obj> fields = new ArrayList<Obj>();
    for (Expr field : expr.getFields()) {
      fields.add(convert(field));
    }
    Obj fieldsArray = mInterpreter.createArray(fields);
    
    return construct("Tuple", fieldsArray);
  }

  @Override
  public Obj visit(TypeofExpr expr, Void dummy) {
    return construct("Typeof", convert(expr.getBody()));
  }

  @Override
  public Obj visit(UnquoteExpr expr, Void dummy) {
    // TODO(bob): Check that it evaluates to an expression?
    return mInterpreter.evaluate(expr.getBody(), mContext);
  }

  @Override
  public Obj visit(UnsafeCastExpr expr, Void dummy) {
    Obj type = convert(expr.getType());
    Obj value = convert(expr.getValue());
    
    return construct("UnsafeCast", type, value);
  }

  @Override
  public Obj visit(VariableExpr expr, Void dummy) {
    return construct("Variable", mInterpreter.createString(expr.getName()),
        convert(expr.getValue()));
  }
  
  private ExprConverter(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  private Obj convert(Expr expr) {
    if (expr == null) return mInterpreter.nothing();
    return expr.accept(this, null);
  }
  
  private Obj construct(String className, Obj... args) {
    Obj classObj = mInterpreter.getGlobal(className + "Expression");
    
    Obj arg;
    if (args.length == 0) {
      arg = mInterpreter.nothing();
    } else if (args.length == 1) {
      arg = args[0];
    } else {
      arg = mInterpreter.createTuple(args);
    }
    
    return mInterpreter.invokeMethod(classObj, "new", arg);
  }

  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
