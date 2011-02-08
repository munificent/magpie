package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
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
    if (exprClass == interpreter.getGlobal("AssignExpression")) {
      return convertAssignExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BlockExpression")) {
      return convertBlockExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BoolExpression")) {
      return convertBoolExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("BreakExpression")) {
      return convertBreakExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("CallExpression")) {
      return convertCallExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("FunctionExpression")) {
      return convertFunctionExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("IntExpression")) {
      return convertIntExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("LoopExpression")) {
      return convertLoopExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("MatchExpression")) {
      return convertMatchExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("MessageExpression")) {
      return convertMessageExpr(interpreter, expr);
    } else if (exprClass == interpreter.getGlobal("NothingExpression")) {
      return convertNothingExpr(interpreter, expr);
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
  
  private static Position convertPosition(Interpreter interpreter, Obj expr) {
    Obj position = expr.getField("position");
    Expect.notNull(position);
    String file   = position.getField("file").asString();
    int startLine = position.getField("startLine").asInt();
    int startCol  = position.getField("startCol").asInt();
    int endLine   = position.getField("endLine").asInt();
    int endCol    = position.getField("endCol").asInt();
    return new Position(file, startLine, startCol, endLine, endCol);
  }

  private static Expr convertAssignExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Obj receiverObj = expr.getField("receiver");
    Expr receiver;
    if (receiverObj == interpreter.nothing()) {
      receiver = null;
    } else {
      receiver = convert(interpreter, receiverObj);
    }
    String name = expr.getField("name").asString();
    Expr value = convert(interpreter, expr.getField("value"));
    return Expr.assign(position, receiver, name, value);
  }
  
  private static Expr convertBlockExpr(Interpreter interpreter, Obj expr) {
    List<Expr> exprs = convertArray(interpreter, expr.getField("expressions"));
    
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
    Position position = convertPosition(interpreter, expr);
    boolean value = expr.getField("value").asBool();
    return Expr.bool(position, value);
  }

  private static Expr convertBreakExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    return Expr.break_(position);
  }

  private static Expr convertCallExpr(Interpreter interpreter, Obj expr) {
    Expr target = convert(interpreter, expr.getField("target"));
    List<Expr> typeArgs = convertArray(interpreter, expr.getField("typeArgs"));
    Expr argument = convert(interpreter, expr.getField("argument"));
    return Expr.call(target, typeArgs, argument);
  }

  private static Expr convertFunctionExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Obj typeObj = expr.getField("functionType");
    FunctionType type = convertFunctionType(interpreter, typeObj);
    
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.fn(position, type, body);
  }

  private static FunctionType convertFunctionType(
      Interpreter interpreter, Obj typeObj) {
    Expr returnType = convert(interpreter, typeObj.getField("returnType"));

    List<Pair<String, Expr>> typeParams = new ArrayList<Pair<String, Expr>>();
    Obj typeParamsObj = typeObj.getField("typeParams");
    for (Obj typeParam : typeParamsObj.asArray()) {
      String name = typeParam.getTupleField(0).asString();
      Expr constraint = convert(interpreter, typeParam.getTupleField(1));
      typeParams.add(new Pair<String, Expr>(name, constraint));
    }
    
    Obj patternObj = typeObj.getField("pattern");
    Pattern pattern = PatternConverter.convert(interpreter, patternObj);
    
    return new FunctionType(typeParams, pattern, returnType);
  }
  
  private static Expr convertIntExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    int value = expr.getField("value").asInt();
    return Expr.int_(position, value);
  }
  
  private static Expr convertLoopExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.loop(position, body);
  }
  
  private static Expr convertMatchExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr value = convert(interpreter, expr.getField("value"));
    List<MatchCase> cases = new ArrayList<MatchCase>();
    for (Obj matchCase : expr.getField("cases").asArray()) {
      Pattern pattern = PatternConverter.convert(
          interpreter, matchCase.getField("pattern"));
      Expr body = convert(interpreter, matchCase.getField("body"));
      cases.add(new MatchCase(pattern, body));
    }
    
    return Expr.match(position, value, cases);
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
    Position position = convertPosition(interpreter, expr);
    return Expr.nothing(position);
  }
  
  private static Expr convertQuotationExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.quote(position, body);
  }

  private static Expr convertRecordExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    List<Obj> fieldObjs = expr.getField("fields").asArray();
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Obj field : fieldObjs) {
      String name = field.getTupleField(0).asString();
      Expr value = convert(interpreter, field.getTupleField(1));
      fields.add(new Pair<String, Expr>(name, value));
    }
    return Expr.record(position, fields);
  }

  private static Expr convertReturnExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr value = convert(interpreter, expr.getField("value"));
    return new ReturnExpr(position, value);
  }

  private static Expr convertScopeExpr(Interpreter interpreter, Obj expr) {
    Expr body = convert(interpreter, expr.getField("body"));
    return Expr.scope(body);
  }
  
  private static Expr convertStringExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    String value = expr.getField("value").asString();
    return Expr.string(position, value);
  }
  
  private static Expr convertThisExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    return Expr.this_(position);
  }
  
  private static Expr convertTupleExpr(Interpreter interpreter, Obj expr) {
    List<Expr> fields = convertArray(interpreter, expr.getField("fields"));

    return Expr.tuple(fields);
  }

  private static Expr convertTypeofExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr body = convert(interpreter, expr.getField("body"));
    return new TypeofExpr(position, body);
  }

  private static Expr convertUnsafeCastExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr type = convert(interpreter, expr.getField("type"));
    Expr value = convert(interpreter, expr.getField("value"));
    return new UnsafeCastExpr(position, type, value);
  }
  
  private static Expr convertVarExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Pattern pattern = PatternConverter.convert(interpreter,
        expr.getField("pattern"));
    Expr value = convert(interpreter, expr.getField("value"));
    return Expr.var(position, pattern, value);
  }
  
  private static List<Expr> convertArray(Interpreter interpreter, Obj array) {
    List<Expr> exprs = new ArrayList<Expr>();
    for (Obj blockExpr : array.asArray()) {
      exprs.add(convert(interpreter, blockExpr));
    }
    return exprs;
  }

  

  @Override
  public Obj visit(AssignExpr expr, Void dummy) {
    return construct("AssignExpression",
        "position", expr.getPosition(),
        "receiver", expr.getReceiver(),
        "name",     expr.getName(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(BlockExpr expr, Void dummy) {
    return construct("BlockExpression",
        "expressions",     convert(expr.getExpressions()),
        "catchExpression", expr.getCatch());
  }

  @Override
  public Obj visit(BoolExpr expr, Void dummy) {
    return construct("BoolExpression",
        "position", expr.getPosition(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(BreakExpr expr, Void dummy) {
    return construct("BreakExpression",
        "position", expr.getPosition());
  }

  @Override
  public Obj visit(CallExpr expr, Void dummy) {
    return construct("CallExpression",
        "target",   expr.getTarget(),
        "typeArgs", convert(expr.getTypeArgs()),
        "argument", expr.getArg());
  }

  @Override
  public Obj visit(FnExpr expr, Void dummy) {
    FunctionType type = expr.getType();
    List<Obj> typeParams = new ArrayList<Obj>();
    for (Pair<String, Expr> typeParam : type.getTypeParams()) {
      Obj name = mInterpreter.createString(typeParam.getKey());
      Obj constraint = convert(typeParam.getValue());
      typeParams.add(mInterpreter.createTuple(name, constraint));
    }
    Obj typeParamsObj = mInterpreter.createArray(typeParams);
    
    Obj typeObj = construct("FunctionTypeExpression",
        "typeParams", typeParamsObj,
        "pattern",    type.getPattern(),
        "returnType", type.getReturnType());
    
    return construct("FunctionExpression",
        "position",     expr.getPosition(),
        "functionType", typeObj,
        "body",         expr.getBody());
  }

  @Override
  public Obj visit(IntExpr expr, Void dummy) {
    return construct("IntExpression",
        "position", expr.getPosition(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, Void dummy) {
    return construct("LoopExpression",
        "position", expr.getPosition(),
        "body",     expr.getBody());
  }

  @Override
  public Obj visit(MatchExpr expr, Void dummy) {
    List<Obj> cases = new ArrayList<Obj>();
    for (MatchCase matchCase : expr.getCases()) {
      cases.add(construct("MatchCase",
          "pattern", matchCase.getPattern(),
          "body", matchCase.getBody()));
    }
    
    return construct("MatchExpression",
        "position", expr.getPosition(),
        "value",    expr.getValue(),
        "cases",    mInterpreter.createArray(cases));
  }

  @Override
  public Obj visit(MessageExpr expr, Void dummy) {
    return construct("MessageExpression",
        "receiver", expr.getReceiver(),
        "name",     expr.getName());
  }

  @Override
  public Obj visit(NothingExpr expr, Void dummy) {
    return construct("NothingExpression",
        "position", expr.getPosition());
  }

  @Override
  public Obj visit(QuotationExpr expr, Void dummy) {
    return construct("QuotationExpression",
        "position", expr.getPosition(),
        "body",     expr.getBody());
  }

  @Override
  public Obj visit(RecordExpr expr, Void dummy) {
    List<Obj> fields = new ArrayList<Obj>();
    for (Pair<String, Expr> field : expr.getFields()) {
      Obj name = mInterpreter.createString(field.getKey());
      Obj value = convert(field.getValue());
      fields.add(mInterpreter.createTuple(name, value));
    }

    return construct("RecordExpression",
        "position", expr.getPosition(),
        "fields",   mInterpreter.createArray(fields));
  }

  @Override
  public Obj visit(ReturnExpr expr, Void dummy) {
    return construct("ReturnExpression",
        "position", expr.getPosition(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(ScopeExpr expr, Void dummy) {
    return construct("ScopeExpression",
        "body", expr.getBody());
  }

  @Override
  public Obj visit(StringExpr expr, Void dummy) {
    return construct("StringExpression",
        "position", expr.getPosition(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(ThisExpr expr, Void dummy) {
    return construct("ThisExpression",
        "position", expr.getPosition());
  }

  @Override
  public Obj visit(TupleExpr expr, Void dummy) {
    return construct("TupleExpression",
        "fields", convert(expr.getFields()));
  }

  @Override
  public Obj visit(TypeofExpr expr, Void dummy) {
    return construct("TypeofExpression",
        "position", expr.getPosition(),
        "body",     expr.getBody());
  }

  @Override
  public Obj visit(UnquoteExpr expr, Void dummy) {
    // TODO(bob): Check that it evaluates to an expression?
    Obj value = mInterpreter.evaluate(expr.getBody(), mContext);
    
    // If the unquoted value is a primitive object, automatically promote it to
    // a corresponding literal.
    if (value.getClassObj() == mInterpreter.getBoolClass()) {
      value = construct("BoolExpression",
          "position", expr.getPosition(),
          "value",    value);
    } else if (value.getClassObj() == mInterpreter.getIntClass()) {
      value = construct("IntExpression",
          "position", expr.getPosition(),
          "value",    value);
    } else if (value.getClassObj() == mInterpreter.getStringClass()) {
      value = construct("StringExpression",
          "position", expr.getPosition(),
          "value",    value);
    } else if (value.getClassObj() == mInterpreter.getNothingClass()) {
      value = construct("NothingExpression",
          "position", expr.getPosition());
    }
    
    return value;
  }

  @Override
  public Obj visit(UnsafeCastExpr expr, Void dummy) {
    return construct("UnsafeCastExpression",
        "position", expr.getPosition(),
        "type",     expr.getType(),
        "value",    expr.getValue());
  }

  @Override
  public Obj visit(VariableExpr expr, Void dummy) {
    return construct("VariableExpression",
        "position", expr.getPosition(),
        "pattern",  expr.getPattern(),
        "value",    expr.getValue());
  }
  
  private Obj construct(String className, Object... args) {
    Map<String, Obj> fields = new HashMap<String, Obj>();
    
    for (int i = 0; i < args.length; i += 2) {
      String name = (String) args[i];
      
      Obj value;
      Object rawValue = args[i + 1];
      if (rawValue == null) {
        value = mInterpreter.nothing();
      } else if (rawValue instanceof Boolean) {
        value = mInterpreter.createBool((Boolean) rawValue);
      } else if (rawValue instanceof Integer) {
        value = mInterpreter.createInt((Integer) rawValue);
      } else if (rawValue instanceof String) {
        value = mInterpreter.createString((String) rawValue);
      } else if (rawValue instanceof Expr) {
        value = convert((Expr) rawValue);
      } else if (rawValue instanceof Position) {
        value = convert((Position) rawValue);
      } else if (rawValue instanceof Pattern) {
        value = PatternConverter.convert(
            (Pattern) rawValue, mInterpreter, mContext);
      } else {
        value = (Obj) rawValue;
      }
      
      fields.put(name, value);
    }
    
    return mInterpreter.invokeMethod(mInterpreter.getGlobal(className),
        "construct", mInterpreter.createRecord(fields));
  }
  
  private ExprConverter(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  private Obj convert(Position position) {
    return construct("Position",
        "file",      position.getSourceFile(),
        "startLine", position.getStartLine(),
        "startCol",  position.getStartCol(),
        "endLine",   position.getEndLine(),
        "endCol",    position.getEndCol());
  }
  
  private Obj convert(Expr expr) {
    if (expr == null) return mInterpreter.nothing();
    return expr.accept(this, null);
  }
  
  private Obj convert(List<Expr> exprs) {
    List<Obj> exprObjs = new ArrayList<Obj>();
    for (Expr expr : exprs) {
      exprObjs.add(convert(expr));
    }
    return mInterpreter.createArray(exprObjs);
  }

  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
