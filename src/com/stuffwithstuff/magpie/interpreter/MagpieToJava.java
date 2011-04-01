package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.Expect;
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
    MagpieToJava converter = new MagpieToJava(interpreter);
    return converter.convertExpr(expr);
  }
  
  public static Pattern convertPattern(Interpreter interpreter, Obj pattern) {
    MagpieToJava converter = new MagpieToJava(interpreter);
    return converter.convertPattern(pattern);
  }
  
  public static Token convertToken(Interpreter interpreter, Obj token) {
    MagpieToJava converter = new MagpieToJava(interpreter);
    return converter.convertToken(token);
  }
  
  public static TokenType convertTokenType(Interpreter interpreter, Obj tokenType) {
    MagpieToJava converter = new MagpieToJava(interpreter);
    return converter.convertTokenType(tokenType);
  }
  
  private MagpieToJava(Interpreter interpreter) {
    mInterpreter = interpreter;
  }

  /**
   * Converts a Magpie expression object into a corresponding Java Expr.
   */
  private Expr convertExpr(Obj expr) {
    Expect.notNull(expr);
    
    if (expr == mInterpreter.nothing()) return null;
    
    ClassObj exprClass = expr.getClassObj();
    if (exprClass == mInterpreter.getGlobal("AssignExpression")) {
      return convertAssignExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("BlockExpression")) {
      return convertBlockExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("BoolExpression")) {
      return convertBoolExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("BreakExpression")) {
      return convertBreakExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("CallExpression")) {
      return convertCallExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("FunctionExpression")) {
      return convertFunctionExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("IntExpression")) {
      return convertIntExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("LoopExpression")) {
      return convertLoopExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("MatchExpression")) {
      return convertMatchExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("MessageExpression")) {
      return convertMessageExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("NothingExpression")) {
      return convertNothingExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("QuotationExpression")) {
      return convertQuotationExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("RecordExpression")) {
      return convertRecordExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("ReturnExpression")) {
      return convertReturnExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("ScopeExpression")) {
      return convertScopeExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("StringExpression")) {
      return convertStringExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("ThisExpression")) {
      return convertThisExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("TupleExpression")) {
      return convertTupleExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("UnquoteExpression")) {
      return convertUnquoteExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("UsingExpression")) {
      return convertUsingExpr(expr);
    } else if (exprClass == mInterpreter.getGlobal("VariableExpression")) {
      return convertVarExpr(expr);
    }
    
    throw new IllegalArgumentException("Not a valid expression object.");
  }
  
  private Pattern convertPattern(Obj pattern) {
    if (pattern == mInterpreter.nothing()) return null;
    
    ClassObj patternClass = pattern.getClassObj();
    if (patternClass == mInterpreter.getGlobal("RecordPattern")) {
      return convertRecordPattern(pattern);
    } else if (patternClass == mInterpreter.getGlobal("TuplePattern")) {
      return convertTuplePattern(pattern);
    } else if (patternClass == mInterpreter.getGlobal("ValuePattern")) {
      return convertValuePattern(pattern);
    } else if (patternClass == mInterpreter.getGlobal("VariablePattern")) {
      return convertVariablePattern(pattern);
    }
    
    throw new IllegalArgumentException("Not a valid expression object.");
  }

  private Expr convertAssignExpr(Obj expr) {
    return Expr.assign(
        getPosition(expr),
        getExpr(expr, "receiver"),
        getString(expr, "name"),
        getExpr(expr, "value"));
  }
  
  private Expr convertBlockExpr(Obj expr) {
    return Expr.block(
        getExprArray(expr, "expressions"),
        getExpr(expr, "catchExpression"));
  }

  private Expr convertBoolExpr(Obj expr) {
    return Expr.bool(
        getPosition(expr),
        getBool(expr, "value"));
  }

  private Expr convertBreakExpr(Obj expr) {
    return Expr.break_(
        getPosition(expr));
  }

  private Expr convertCallExpr(Obj expr) {
    return Expr.call(
        getExpr(expr, "target"),
        getExpr(expr, "argument"));
  }

  private Expr convertFunctionExpr(Obj expr) {
    Obj patternObj = getMember(expr, "pattern");
    Pattern pattern = convertPattern(patternObj);
    
    return Expr.fn(
        getPosition(expr),
        pattern,
        getExpr(expr, "body"));
  }
  
  private Expr convertIntExpr(Obj expr) {
    return Expr.int_(
        getPosition(expr),
        getInt(expr, "value"));
  }
  
  private Expr convertLoopExpr(Obj expr) {
    return Expr.loop(
        getPosition(expr),
        getExpr(expr, "body"));
  }
  
  private Expr convertMatchExpr(Obj expr) {
    List<MatchCase> cases = new ArrayList<MatchCase>();
    for (Obj matchCase : getArray(expr, "cases")) {
      Pattern pattern = convertPattern(matchCase.getField("pattern"));
      Expr body = convertExpr(matchCase.getField("body"));
      cases.add(new MatchCase(pattern, body));
    }
    
    return Expr.match(
        getPosition(expr),
        getExpr(expr, "value"),
        cases);
  }
  
  private Expr convertMessageExpr(Obj expr) {
    return Expr.message(
        getPosition(expr),
        getExpr(expr, "receiver"),
        getString(expr, "name"));
  }
  
  private Expr convertNothingExpr(Obj expr) {
    return Expr.nothing(
        getPosition(expr));
  }
  
  private Expr convertQuotationExpr(Obj expr) {
    return Expr.quote(
        getPosition(expr),
        getExpr(expr, "body"));
  }

  private Expr convertRecordExpr(Obj expr) {
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Obj field : getArray(expr, "fields")) {
      String name = field.getTupleField(0).asString();
      Expr value = convertExpr(field.getTupleField(1));
      fields.add(new Pair<String, Expr>(name, value));
    }
    return Expr.record(
        getPosition(expr),
        fields);
  }

  private Expr convertReturnExpr(Obj expr) {
    return new ReturnExpr(
        getPosition(expr),
        getExpr(expr, "value"));
  }

  private Expr convertScopeExpr(Obj expr) {
    return Expr.scope(
        getExpr(expr, "body"));
  }
  
  private Expr convertStringExpr(Obj expr) {
    return Expr.string(
        getPosition(expr),
        getString(expr, "value"));
  }
  
  private Expr convertThisExpr(Obj expr) {
    return Expr.this_(
        getPosition(expr));
  }
  
  private Expr convertTupleExpr(Obj expr) {
    return Expr.tuple(
        getExprArray(expr, "fields"));
  }

  private Expr convertUnquoteExpr(Obj expr) {
    return new UnquoteExpr(
        getPosition(expr),
        getExpr(expr, "body"));
  }
  
  private Expr convertUsingExpr(Obj expr) {
    return Expr.string(
        getPosition(expr),
        getString(expr, "name"));
  }

  private Expr convertVarExpr(Obj expr) {
    return Expr.var(
        getPosition(expr),
        getPattern(expr, "pattern"),
        getExpr(expr, "value"));
  }
  
  private Pattern convertRecordPattern(Obj pattern) {
    List<Obj> fieldObjs = getArray(pattern, "fields");
    List<Pair<String, Pattern>> fields = new ArrayList<Pair<String, Pattern>>();
    for (Obj field : fieldObjs) {
      String name = field.getTupleField(0).asString();
      Pattern fieldPattern = convertPattern(field.getTupleField(1));
      fields.add(new Pair<String, Pattern>(name, fieldPattern));
    }
    
    return Pattern.record(fields);
  }
  
  private Pattern convertTuplePattern(Obj pattern) {
    List<Obj> fieldObjs = getArray(pattern, "fields");
    List<Pattern> fields = new ArrayList<Pattern>();
    for (Obj field : fieldObjs) {
      fields.add(convertPattern(field));
    }
    
    return Pattern.tuple(fields);
  }
  
  private Pattern convertValuePattern(Obj pattern) {
    return Pattern.value(
        getExpr(pattern, "value"));
  }
  
  private Pattern convertVariablePattern(Obj pattern) {
    return Pattern.variable(
        getString(pattern, "name"),
        getExpr(pattern, "typeExpr"));
  }

  private Position convertPosition(Obj position) {
    return new Position(
        getString(position, "file"),
        getInt(position, "startLine"),
        getInt(position, "startCol"),
        getInt(position, "endLine"),
        getInt(position, "endCol"));
  }
  
  private Token convertToken(Obj token) {
    Position position = getPosition(token);
    TokenType type = convertTokenType(getMember(token, "tokenType"));
    Object value = getMember(token, "value").getValue();
    
    return new Token(position, type, value);
  }

  private TokenType convertTokenType(Obj tokenType) {
    Obj value = getMember(tokenType, "value");
    
    // Note: the values here must be kept in sync with the order that they
    // are defined in Token.mag.
    TokenType type;
    switch (value.asInt()) {
    case 0: type = TokenType.LEFT_PAREN; break;
    case 1: type = TokenType.RIGHT_PAREN; break;
    case 2: type = TokenType.LEFT_BRACKET; break;
    case 3: type = TokenType.RIGHT_BRACKET; break;
    case 4: type = TokenType.LEFT_BRACE; break;
    case 5: type = TokenType.RIGHT_BRACE; break;
    case 6: type = TokenType.COLON; break;
    case 7: type = TokenType.COMMA; break;
    case 8: type = TokenType.DOT; break;
    case 9: type = TokenType.LINE; break;
    case 10: type = TokenType.NAME; break;
    case 11: type = TokenType.FIELD; break;
    case 12: type = TokenType.BOOL; break;
    case 13: type = TokenType.INT; break;
    case 14: type = TokenType.STRING; break;
    case 15: type = TokenType.EOF; break;
    default:
      // TODO(bob): Better error reporting.
      mInterpreter.error("ParseError");
      type = TokenType.EOF;
    }
    
    return type;
  }
  
  private List<Obj> getArray(Obj obj, String name) {
    return getMember(obj, name).asArray();
  }
  
  private List<Expr> getExprArray(Obj obj, String name) {
    List<Obj> array = getMember(obj, name).asArray();
    List<Expr> exprs = new ArrayList<Expr>();
    for (Obj blockExpr : array) {
      exprs.add(convertExpr(blockExpr));
    }
    return exprs;
  }

  private boolean getBool(Obj obj, String member) {
    Obj value = getMember(obj, member);
    return value.asBool();
  }

  private int getInt(Obj obj, String member) {
    Obj value = getMember(obj, member);
    return value.asInt();
  }
  
  private String getString(Obj obj, String member) {
    Obj value = getMember(obj, member);
    return value.asString();
  }
  
  private Position getPosition(Obj obj) {
    return convertPosition(getMember(obj, "position"));
  }
  
  private Expr getExpr(Obj obj, String name) {
    return convertExpr(getMember(obj, name));
  }
  
  private Pattern getPattern(Obj obj, String name) {
    return convertPattern(getMember(obj, name));
  }
  
  private Obj getMember(Obj obj, String name) {
    return mInterpreter.getQualifiedMember(Position.none(), obj, null, name);
  }
  
  private Interpreter mInterpreter;
}
