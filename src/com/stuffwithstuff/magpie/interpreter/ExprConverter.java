package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.NotImplementedException;
import com.stuffwithstuff.magpie.util.Pair;

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
  public static Expr convert(Interpreter interpreter, Obj expr) {
    Expect.notNull(expr);
    
    if (expr == interpreter.nothing()) return null;
    
    ClassObj exprClass = expr.getClassObj();
    // TODO(bob): Fill in other expression types.
    // TODO(bob): Support position information in Magpie parser.
    if (exprClass == interpreter.getGlobal("AndExpression")) {
      return convertAndExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("ApplyExpression")) {
      return convertApplyExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("AssignExpression")) {
      return convertAssignExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BlockExpression")) {
      return convertBlockExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BoolExpression")) {
      return convertBoolExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BreakExpression")) {
      return convertBreakExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("FunctionExpression")) {
      return convertFunctionExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("IfExpression")) {
      return convertIfExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("IntExpression")) {
      return convertIntExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("LoopExpression")) {
      return convertLoopExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("MessageExpression")) {
      return convertMessageExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("NothingExpression")) {
      return convertNothingExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("OrExpression")) {
      return convertOrExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("QuotationExpression")) {
      return convertQuotationExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("RecordExpression")) {
      return convertRecordExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("ReturnExpression")) {
      return convertReturnExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("ScopeExpression")) {
      return convertScopeExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("StringExpression")) {
      return convertStringExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("ThisExpression")) {
      return convertThisExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("TupleExpression")) {
      return convertTupleExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("TypeofExpression")) {
      return convertTypeofExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("UnsafeCastExpression")) {
      return convertUnsafeCastExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("VariableExpression")) {
      return convertVarExpr(interpreter, expr);
    }
    
    // TODO(bob): Add better error-handling.
    throw new NotImplementedException(
        "Other expression types not implemented yet!");
  }

  private static Expr convertAndExpr(Interpreter interpreter, Obj expr) {
    Expr left = convert(interpreter, expr.getField("left"));
    Expr right = convert(interpreter, expr.getField("right"));
    return Expr.and(left, right);
  }

  private static Expr convertApplyExpr(Interpreter interpreter, Obj expr) {
    Expr target = convert(interpreter, expr.getField("target"));
    Expr argument = convert(interpreter, expr.getField("argument"));
    boolean isStatic = expr.getField("static?").asBool();
    return Expr.apply(target, argument, isStatic);
  }

  private static Expr convertAssignExpr(Interpreter interpreter, Obj expr) {
    Obj receiverObj = expr.getField("receiver");
    Expr receiver;
    if (receiverObj == interpreter.nothing()) {
      receiver = null;
    } else {
      receiver = convert(interpreter, receiverObj);
    }
    String name = expr.getField("name").asString();
    Expr value = convert(interpreter, expr.getField("value"));
    return Expr.assign(Position.none(), receiver, name, value);
  }
  
  private static Expr convertBlockExpr(Interpreter interpreter, Obj expr) {
    List<Obj> exprObjs = expr.getField("expressions").asArray();
    List<Expr> exprs = new ArrayList<Expr>();
    for (Obj blockExpr : exprObjs) {
      exprs.add(convert(interpreter, blockExpr));
    }
    
    Obj catchObj = expr.getField("catchExpression");
    Expr catchExpr;
    if (catchObj == interpreter.nothing()) {
      catchExpr = null;
    } else {
      catchExpr = convert(interpreter, catchObj);
    }
    return Expr.block(exprs, catchExpr);
  }

  private static Expr convertBoolExpr(Interpreter interpreter, Obj expr) {
    boolean value = expr.getField("value").asBool();
    return Expr.bool(value);
  }

  private static Expr convertBreakExpr(Interpreter interpreter, Obj expr) {
    return Expr.break_(Position.none());
  }

  private static Expr convertFunctionExpr(Interpreter interpreter, Obj expr) {
    Obj typeObj = expr.getField("functionType");
    Expr returnType = convert(interpreter, typeObj.getField("returnType"));

    Obj patternObj = typeObj.getField("pattern");
    Pattern pattern = PatternConverter.convert(interpreter, patternObj);
    
    boolean isStatic = typeObj.getField("static?").asBool();
    
    FunctionType type = new FunctionType(pattern, returnType, isStatic);
    
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.fn(Position.none(), type, body);
  }

  private static Expr convertIfExpr(Interpreter interpreter, Obj expr) {
    Obj nameObj = expr.getField("name");
    String name;
    if (nameObj == interpreter.nothing()) {
      name = null;
    } else {
      name = nameObj.asString();
    }
    Expr condition = convert(interpreter, expr.getField("condition"));
    Expr thenArm = convert(interpreter, expr.getField("thenArm"));
    Expr elseArm = convert(interpreter, expr.getField("elseArm"));
    return new IfExpr(Position.none(), name, condition, thenArm, elseArm);
  }

  private static Expr convertIntExpr(Interpreter interpreter, Obj expr) {
    int value = expr.getField("value").asInt();
    return Expr.int_(value);
  }
  
  private static Expr convertLoopExpr(Interpreter interpreter, Obj expr) {
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.loop(body.getPosition(), body);
  }
  
  private static Expr convertMessageExpr(Interpreter interpreter, Obj expr) {
    Obj receiverObj = expr.getField("receiver");
    Expr receiver;
    if (receiverObj == interpreter.nothing()) {
      receiver = null;
    } else {
      receiver = convert(interpreter, receiverObj);
    }
    String name = expr.getField("name").asString();
    return Expr.message(receiver, name);
  }
  
  private static Expr convertNothingExpr(Interpreter interpreter, Obj expr) {
    return Expr.nothing();
  }
  
  private static Expr convertOrExpr(Interpreter interpreter, Obj expr) {
    Expr left = convert(interpreter, expr.getField("left"));
    Expr right = convert(interpreter, expr.getField("right"));
    return Expr.or(left, right);
  }

  private static Expr convertQuotationExpr(Interpreter interpreter, Obj expr) {
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.quote(Position.none(), body);
  }

  private static Expr convertRecordExpr(Interpreter interpreter, Obj expr) {
    List<Obj> fieldObjs = expr.getField("fields").asArray();
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Obj field : fieldObjs) {
      String name = field.getTupleField(0).asString();
      Expr value = convert(interpreter, field.getTupleField(1));
      fields.add(new Pair<String, Expr>(name, value));
    }
    return new RecordExpr(Position.none(), fields);
  }

  private static Expr convertReturnExpr(Interpreter interpreter, Obj expr) {
    Expr value = convert(interpreter, expr.getField("value"));
    return new ReturnExpr(Position.none(), value);
  }

  private static Expr convertScopeExpr(Interpreter interpreter, Obj expr) {
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.scope(body);
  }
  
  private static Expr convertStringExpr(Interpreter interpreter, Obj expr) {
    String value = expr.getField("value").asString();
    return Expr.string(value);
  }
  
  private static Expr convertThisExpr(Interpreter interpreter, Obj expr) {
    return Expr.this_(Position.none());
  }
  
  private static Expr convertTupleExpr(Interpreter interpreter, Obj expr) {
    List<Obj> fieldObjs = expr.getField("fields").asArray();
    List<Expr> fields = new ArrayList<Expr>();
    for (Obj field : fieldObjs) {
      fields.add(convert(interpreter, field));
    }
    return Expr.tuple(fields);
  }

  private static Expr convertTypeofExpr(Interpreter interpreter, Obj expr) {
    Expr body = convert(interpreter, expr.getField("body"));
    return new TypeofExpr(Position.none(), body);
  }

  private static Expr convertUnsafeCastExpr(Interpreter interpreter, Obj expr) {
    Expr type = convert(interpreter, expr.getField("type"));
    Expr value = convert(interpreter, expr.getField("value"));
    return new UnsafeCastExpr(Position.none(), type, value);
  }
  
  private static Expr convertVarExpr(Interpreter interpreter, Obj expr) {
    Pattern pattern = PatternConverter.convert(interpreter,
        expr.getField("pattern"));
    Expr value = convert(interpreter, expr.getField("value"));
    return Expr.var(Position.none(), pattern, value);
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
    return construct("Apply", target, arg,
        mInterpreter.createBool(expr.isStatic()));
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
    Obj catchExpr = convert(expr.getCatch());
    
    return construct("Block", exprsArray, catchExpr);
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
  public Obj visit(FnExpr expr, Void dummy) {
    Obj pattern = PatternConverter.convert(expr.getType().getPattern(),
        mInterpreter, mContext);
    Obj returnType = convert(expr.getType().getReturnType());
    Obj type = construct("FunctionType",
        pattern,
        returnType, 
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
    Obj body = convert(expr.getBody());
    return construct("Loop", body);
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
  public Obj visit(QuotationExpr expr, Void dummy) {
    return construct("Quotation", convert(expr.getBody()));
  }

  @Override
  public Obj visit(RecordExpr expr, Void dummy) {
    List<Obj> fields = new ArrayList<Obj>();
    for (Pair<String, Expr> field : expr.getFields()) {
      Obj name = mInterpreter.createString(field.getKey());
      Obj value = convert(field.getValue());
      fields.add(mInterpreter.createTuple(name, value));
    }
    Obj fieldsArray = mInterpreter.createArray(fields);
    
    return construct("Record", fieldsArray);
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
    Obj value = mInterpreter.evaluate(expr.getBody(), mContext);
    
    // If the unquoted value is a primitive object, automatically promote it to
    // a corresponding literal.
    if (value.getClassObj() == mInterpreter.getBoolClass()) {
      value = construct("Bool", value);
    } else if (value.getClassObj() == mInterpreter.getIntClass()) {
      value = construct("Int", value);
    } else if (value.getClassObj() == mInterpreter.getStringClass()) {
      value = construct("String", value);
    } else if (value.getClassObj() == mInterpreter.getNothingClass()) {
      value = construct("Nothing", value);
    }
    
    return value;
  }

  @Override
  public Obj visit(UnsafeCastExpr expr, Void dummy) {
    Obj type = convert(expr.getType());
    Obj value = convert(expr.getValue());
    
    return construct("UnsafeCast", type, value);
  }

  @Override
  public Obj visit(VariableExpr expr, Void dummy) {
    Obj pattern = PatternConverter.convert(
        expr.getPattern(), mInterpreter, mContext);
    
    return construct("Variable", pattern, convert(expr.getValue()));
  }
  
  private Obj construct(String className, Obj... args) {
    return mInterpreter.construct(className + "Expression", args);
  }
  
  private ExprConverter(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  private Obj convert(Expr expr) {
    if (expr == null) return mInterpreter.nothing();
    return expr.accept(this, null);
  }

  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
