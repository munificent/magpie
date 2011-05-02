package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

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
    if (exprClass == getClass("AssignExpression")) {
      return Expr.assign(
          getPosition(expr),
          getString(expr, "name"),
          getExpr(expr, "value"));
    } else if (exprClass == getClass("BoolExpression")) {
      return Expr.bool(
          getPosition(expr),
          getBool(expr, "value"));
    } else if (exprClass == getClass("BreakExpression")) {
      return Expr.break_(
          getPosition(expr));
    } else if (exprClass == getClass("ClassExpression")) {
      List<String> parents = new ArrayList<String>();
      for (Obj parent : expr.getField("parents").asList()) {
        parents.add(parent.asString());
      }
      
      Map<String, Field> fields = new HashMap<String, Field>();
      for (Obj entry : expr.getField("fields").asList()) {
        Obj fieldObj = entry.getField(1);
        Field field = new Field(
            getBool(fieldObj, "mutable?"),
            getExpr(fieldObj, "initializer"),
            getExpr(fieldObj, "type"));
        fields.put(entry.getField(0).asString(), field);
      }

      return Expr.class_(
          getPosition(expr),
          getString(expr, "doc"),
          getString(expr, "name"),
          parents,
          fields);
    } else if (exprClass == getClass("CallExpression")) {
      return Expr.call(
          getPosition(expr),
          getExpr(expr, "receiver"),
          getString(expr, "name"),
          getExpr(expr, "argument"));
    } else if (exprClass == getClass("DefineExpression")) {
      return Expr.define(
          getPosition(expr),
          getPattern(expr, "pattern"),
          getExpr(expr, "value"));
    } else if (exprClass == getClass("FunctionExpression")) {
      return Expr.fn(
          getPosition(expr),
          getString(expr, "doc"),
          getPattern(expr, "pattern"),
          getExpr(expr, "body"));
    } else if (exprClass == getClass("ImportExpression")) {
      return Expr.import_(
          getPosition(expr),
          getString(expr, "scheme"),
          getString(expr, "module"),
          getString(expr, "name"),
          getString(expr, "rename"));
    } else if (exprClass == getClass("IntExpression")) {
      return Expr.int_(
          getPosition(expr),
          getInt(expr, "value"));
    } else if (exprClass == getClass("ListExpression")) {
      return Expr.list(
          getPosition(expr),
          getExprList(expr, "elements"));
    } else if (exprClass == getClass("LoopExpression")) {
      return Expr.loop(
          getPosition(expr),
          getExpr(expr, "body"));
    } else if (exprClass == getClass("MatchExpression")) {
      return Expr.match(
          getPosition(expr),
          getExpr(expr, "value"),
          getMatchCaseList(expr, "cases"));
    } else if (exprClass == getClass("MethodExpression")) {
      return Expr.method(
          getPosition(expr),
          getString(expr, "doc"),
          getString(expr, "name"),
          getPattern(expr, "pattern"),
          getExpr(expr, "body"));
    } else if (exprClass == getClass("NameExpression")) {
      return Expr.name(
          getPosition(expr),
          getString(expr, "name"));
    } else if (exprClass == getClass("NothingExpression")) {
      return Expr.nothing(
          getPosition(expr));
    } else if (exprClass == getClass("RecordExpression")) {
      List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
      for (Obj field : getList(expr, "fields")) {
        String name = field.getField(0).asString();
        Expr value = convertExpr(field.getField(1));
        fields.add(new Pair<String, Expr>(name, value));
      }
      return Expr.record(
          getPosition(expr),
          fields);
    } else if (exprClass == getClass("ReturnExpression")) {
      return Expr.return_(
          getPosition(expr),
          getExpr(expr, "value"));
    } else if (exprClass == getClass("ScopeExpression")) {
      return Expr.scope(
//          getPosition(expr),
          getExpr(expr, "body"),
          getMatchCaseList(expr, "catches"));
    } else if (exprClass == getClass("SequenceExpression")) {
      return Expr.sequence(
//          getPosition(expr),
          getExprList(expr, "expressions"));
    } else if (exprClass == getClass("StringExpression")) {
      return Expr.string(
          getPosition(expr),
          getString(expr, "value"));
    } else if (exprClass == getClass("ThrowExpression")) {
      return Expr.throw_(
          getPosition(expr),
          getExpr(expr, "value"));
    }
    
    throw new IllegalArgumentException("Not a valid expression object.");
  }
  
  private MatchCase convertMatchCase(Obj matchCase) {
    return new MatchCase(
        getPattern(matchCase, "pattern"),
        getExpr(matchCase, "body"));
  }

  private Pattern convertPattern(Obj pattern) {
    if (pattern == mInterpreter.nothing()) return null;
    
    ClassObj patternClass = pattern.getClassObj();

    if (patternClass == getClass("RecordPattern")) {
      Map<String, Pattern> fields = new HashMap<String, Pattern>();
      for (Obj field : getList(pattern, "fields")) {
        String name = field.getField(0).asString();
        Pattern fieldPattern = convertPattern(field.getField(1));
        fields.put(name, fieldPattern);
      }
      return Pattern.record(fields);
    } else if (patternClass == getClass("TypePattern")) {
      return Pattern.type(
          getExpr(pattern, "type"));
    } else if (patternClass == getClass("ValuePattern")) {
      return Pattern.value(
          getExpr(pattern, "value"));
    } else if (patternClass == getClass("VariablePattern")) {
      return Pattern.variable(
          getString(pattern, "name"),
          getPattern(pattern, "pattern"));
    } else if (patternClass == getClass("WildcardPattern")) {
      return Pattern.wildcard();
    }
    
    throw new IllegalArgumentException("Not a valid pattern object.");
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
    TokenType type = convertTokenType(token.getField("tokenType"));
    String text = getString(token, "text");
    Object value = token.getField("value").getValue();
    
    return new Token(position, type, text, value);
  }

  private TokenType convertTokenType(Obj tokenType) {
    String name = getString(tokenType, "name");
    
    return TokenType.valueOf(name);
  }

  private boolean getBool(Obj obj, String name) {
    return obj.getField(name).asBool();
  }
  
  private Expr getExpr(Obj obj, String name) {
    return convertExpr(obj.getField(name));
  }

  private List<Expr> getExprList(Obj obj, String name) {
    List<Expr> exprs = new ArrayList<Expr>();
    for (Obj expr : obj.getField(name).asList()) {
      exprs.add(convertExpr(expr));
    }
    return exprs;
  }

  private int getInt(Obj obj, String name) {
    return obj.getField(name).asInt();
  }
  
  private List<Obj> getList(Obj obj, String name) {
    return obj.getField(name).asList();
  }
  
  private List<MatchCase> getMatchCaseList(Obj obj, String name) {
    List<MatchCase> cases = new ArrayList<MatchCase>();
    for (Obj matchCase : obj.getField(name).asList()) {
      cases.add(convertMatchCase(matchCase));
    }
    return cases;
  }
  
  private Pattern getPattern(Obj obj, String name) {
    return convertPattern(obj.getField(name));
  }
  
  private Position getPosition(Obj obj) {
    return convertPosition(obj.getField("position"));
  }

  private String getString(Obj obj, String name) {
    return obj.getField(name).asString();
  }
  
  private ClassObj getClass(String name) {
    return mInterpreter.getSyntaxModule().getExportedVariables()
        .get(name).asClass();
  }
  
  private final Interpreter mInterpreter;
}
