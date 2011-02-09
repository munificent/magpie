package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.NotImplementedException;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Translates Magpie objects (expressions, patterns, etc.) and converts them to
 * their native Java equivalents.
 */
public class MagpieToJava {
  /**
   * Converts a Magpie expression object into a corresponding Java Expr.
   */
  public static Expr convertExpr(Interpreter interpreter, Obj expr) {
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
  
  public static Pattern convertPattern(Interpreter interpreter, Obj pattern) {
    if (pattern == interpreter.nothing()) return null;
    
    ClassObj patternClass = pattern.getClassObj();
    if (patternClass == interpreter.getGlobal("RecordPattern")) {
      return convertRecordPattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("TuplePattern")) {
      return convertTuplePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("ValuePattern")) {
      return convertValuePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("VariablePattern")) {
      return convertVariablePattern(interpreter, pattern);
    }
    
    // TODO(bob): Add better error-handling.
    throw new NotImplementedException(
        "Other pattern types not implemented yet!");
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
      receiver = convertExpr(interpreter, receiverObj);
    }
    String name = expr.getField("name").asString();
    Expr value = convertExpr(interpreter, expr.getField("value"));
    return Expr.assign(position, receiver, name, value);
  }
  
  private static Expr convertBlockExpr(Interpreter interpreter, Obj expr) {
    List<Expr> exprs = convertArray(interpreter, expr.getField("expressions"));
    
    Obj catchObj = expr.getField("catchExpression");
    Expr catchExpr;
    if (catchObj == interpreter.nothing()) {
      catchExpr = null;
    } else {
      catchExpr = convertExpr(interpreter, catchObj);
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
    Expr target = convertExpr(interpreter, expr.getField("target"));
    List<Expr> typeArgs = convertArray(interpreter, expr.getField("typeArgs"));
    Expr argument = convertExpr(interpreter, expr.getField("argument"));
    return Expr.call(target, typeArgs, argument);
  }

  private static Expr convertFunctionExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Obj typeObj = expr.getField("functionType");
    FunctionType type = convertFunctionType(interpreter, typeObj);
    
    Expr body = convertExpr(interpreter, expr.getField("body"));
    return Expr.fn(position, type, body);
  }

  private static FunctionType convertFunctionType(
      Interpreter interpreter, Obj typeObj) {
    Expr returnType = convertExpr(interpreter, typeObj.getField("returnType"));

    List<Pair<String, Expr>> typeParams = new ArrayList<Pair<String, Expr>>();
    Obj typeParamsObj = typeObj.getField("typeParams");
    for (Obj typeParam : typeParamsObj.asArray()) {
      String name = typeParam.getTupleField(0).asString();
      Expr constraint = convertExpr(interpreter, typeParam.getTupleField(1));
      typeParams.add(new Pair<String, Expr>(name, constraint));
    }
    
    Obj patternObj = typeObj.getField("pattern");
    Pattern pattern = convertPattern(interpreter, patternObj);
    
    return new FunctionType(typeParams, pattern, returnType);
  }
  
  private static Expr convertIntExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    int value = expr.getField("value").asInt();
    return Expr.int_(position, value);
  }
  
  private static Expr convertLoopExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr body = convertExpr(interpreter, expr.getField("body"));
    return Expr.loop(position, body);
  }
  
  private static Expr convertMatchExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr value = convertExpr(interpreter, expr.getField("value"));
    List<MatchCase> cases = new ArrayList<MatchCase>();
    for (Obj matchCase : expr.getField("cases").asArray()) {
      Pattern pattern = convertPattern(
          interpreter, matchCase.getField("pattern"));
      Expr body = convertExpr(interpreter, matchCase.getField("body"));
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
      receiver = convertExpr(interpreter, receiverObj);
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
    Expr body = convertExpr(interpreter, expr.getField("body"));
    return Expr.quote(position, body);
  }

  private static Expr convertRecordExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    List<Obj> fieldObjs = expr.getField("fields").asArray();
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Obj field : fieldObjs) {
      String name = field.getTupleField(0).asString();
      Expr value = convertExpr(interpreter, field.getTupleField(1));
      fields.add(new Pair<String, Expr>(name, value));
    }
    return Expr.record(position, fields);
  }

  private static Expr convertReturnExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr value = convertExpr(interpreter, expr.getField("value"));
    return new ReturnExpr(position, value);
  }

  private static Expr convertScopeExpr(Interpreter interpreter, Obj expr) {
    Expr body = convertExpr(interpreter, expr.getField("body"));
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
    Expr body = convertExpr(interpreter, expr.getField("body"));
    return new TypeofExpr(position, body);
  }

  private static Expr convertUnsafeCastExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Expr type = convertExpr(interpreter, expr.getField("type"));
    Expr value = convertExpr(interpreter, expr.getField("value"));
    return new UnsafeCastExpr(position, type, value);
  }
  
  private static Expr convertVarExpr(Interpreter interpreter, Obj expr) {
    Position position = convertPosition(interpreter, expr);
    Pattern pattern = convertPattern(interpreter,
        expr.getField("pattern"));
    Expr value = convertExpr(interpreter, expr.getField("value"));
    return Expr.var(position, pattern, value);
  }
  
  private static Pattern convertRecordPattern(Interpreter interpreter,
      Obj pattern) {
    List<Obj> fieldObjs = pattern.getField("fields").asArray();
    List<Pair<String, Pattern>> fields = new ArrayList<Pair<String, Pattern>>();
    for (Obj field : fieldObjs) {
      String name = field.getTupleField(0).asString();
      Pattern fieldPattern = convertPattern(interpreter, field.getTupleField(1));
      fields.add(new Pair<String, Pattern>(name, fieldPattern));
    }
    
    return new RecordPattern(fields);
  }
  
  private static Pattern convertTuplePattern(Interpreter interpreter,
      Obj pattern) {
    List<Obj> fieldObjs = pattern.getField("fields").asArray();
    List<Pattern> fields = new ArrayList<Pattern>();
    for (Obj field : fieldObjs) {
      fields.add(convertPattern(interpreter, field));
    }
    
    return new TuplePattern(fields);
  }
  
  private static Pattern convertValuePattern(Interpreter interpreter,
      Obj pattern) {
    Obj value = pattern.getField("value");
    Expr expr = MagpieToJava.convertExpr(interpreter, value);
    return new ValuePattern(expr);
  }
  
  private static Pattern convertVariablePattern(Interpreter interpreter,
      Obj pattern) {
    String name = pattern.getField("name").asString();
    Obj type = pattern.getField("typeExpr");
    Expr expr = null;
    if (type != interpreter.nothing()) {
      expr = MagpieToJava.convertExpr(interpreter, type);
    }
    return new VariablePattern(name, expr);
  }
  
  private static List<Expr> convertArray(Interpreter interpreter, Obj array) {
    List<Expr> exprs = new ArrayList<Expr>();
    for (Obj blockExpr : array.asArray()) {
      exprs.add(convertExpr(interpreter, blockExpr));
    }
    return exprs;
  }
}
