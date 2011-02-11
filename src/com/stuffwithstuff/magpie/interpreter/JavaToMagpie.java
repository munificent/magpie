package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.PatternVisitor;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.Pair;

public class JavaToMagpie {
  public static Obj convert(Interpreter interpreter, Expr expr,
      EvalContext context) {
    
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, context);
    return javaToMagpie.convertExpr(expr);
  }
  
  public static Obj convert(Interpreter interpreter, Pattern pattern,
      EvalContext context) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, context);
    return javaToMagpie.convertPattern(pattern);
  }

  public static Obj convert(Interpreter interpreter, TokenType type) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, null);
    return javaToMagpie.convertTokenType(type);
  }
  
  public static Obj convert(Interpreter interpreter, Token token) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, null);
    return javaToMagpie.convertToken(token);
  }
  
  private class ExprConverter implements ExprVisitor<Obj, Void> {
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
          "expressions",     convertList(expr.getExpressions()),
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
          "typeArgs", convertList(expr.getTypeArgs()),
          "argument", expr.getArg());
    }

    @Override
    public Obj visit(FnExpr expr, Void dummy) {
      FunctionType type = expr.getType();
      List<Obj> typeParams = new ArrayList<Obj>();
      for (Pair<String, Expr> typeParam : type.getTypeParams()) {
        Obj name = mInterpreter.createString(typeParam.getKey());
        Obj constraint = convertObject(typeParam.getValue());
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
          "position", expr.getPosition(),
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
        Obj value = convertObject(field.getValue());
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
          "fields", convertList(expr.getFields()));
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
  }
  
  public class PatternConverter implements PatternVisitor<Obj, Void> {
    @Override
    public Obj visit(RecordPattern pattern, Void dummy) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Pair<String, Pattern> field : pattern.getFields()) {
        Obj name = mInterpreter.createString(field.getKey());
        Obj value = convertObject(field.getValue());
        fields.add(mInterpreter.createTuple(name, value));
      }

      return construct("RecordPattern",
          "fields", mInterpreter.createArray(fields));
    }

    @Override
    public Obj visit(TuplePattern pattern, Void dummy) {
      return construct("TuplePattern",
          "fields", convertList(pattern.getFields()));
    }

    @Override
    public Obj visit(ValuePattern pattern, Void dummy) {
      return construct("ValuePattern",
          "value", pattern.getValue());
    }

    @Override
    public Obj visit(VariablePattern pattern, Void dummy) {
      return construct("VariablePattern",
          "name",     pattern.getName(),
          "typeExpr", pattern.getType());
    }
  }  
  
  private Obj construct(String className, Object... args) {
    Map<String, Obj> fields = new HashMap<String, Obj>();
    
    for (int i = 0; i < args.length; i += 2) {
      fields.put((String) args[i], convertObject(args[i + 1]));
    }
    
    return mInterpreter.invokeMethod(mInterpreter.getGlobal(className),
        "construct", mInterpreter.createRecord(fields));
  }
  
  private Obj convertPosition(Position position) {
    return construct("Position",
        "file",      position.getSourceFile(),
        "startLine", position.getStartLine(),
        "startCol",  position.getStartCol(),
        "endLine",   position.getEndLine(),
        "endCol",    position.getEndCol());
  }
  
  private Obj convertExpr(Expr expr) {
    if (expr == null) return mInterpreter.nothing();
    ExprConverter converter = new ExprConverter();
    return expr.accept(converter, null);
  }

  private Obj convertPattern(Pattern pattern) {
    if (pattern == null) return mInterpreter.nothing();
    
    PatternConverter converter = new PatternConverter();
    return pattern.accept(converter, null);
  }
  
  private Obj convertToken(Token token) {
    if (token == null) return mInterpreter.nothing();
    
    return construct("Token",
        "position",  token.getPosition(),
        "tokenType", token.getType(),
        "value",     token.getValue());
  }
  
  private Obj convertTokenType(TokenType type) {
    // Note: the values here must be kept in sync with the order that they
    // are defined in Token.mag.
    int tokenTypeValue;
    String tokenTypeName;
    switch (type) {
    case LEFT_PAREN: tokenTypeValue = 0; tokenTypeName = "leftParen"; break;
    case RIGHT_PAREN: tokenTypeValue = 1; tokenTypeName = "rightParen"; break;
    case LEFT_BRACKET: tokenTypeValue = 2; tokenTypeName = "leftBracket"; break;
    case RIGHT_BRACKET: tokenTypeValue = 3; tokenTypeName = "rightBracket"; break;
    case LEFT_BRACE: tokenTypeValue = 4; tokenTypeName = "leftBrace"; break;
    case RIGHT_BRACE: tokenTypeValue = 5; tokenTypeName = "leftBrace"; break;
    case COMMA: tokenTypeValue = 6; tokenTypeName = "comma"; break;
    case DOT: tokenTypeValue = 7; tokenTypeName = "dot"; break;
    case EQUALS: tokenTypeValue = 8; tokenTypeName = "equals"; break;
    case LINE: tokenTypeValue = 9; tokenTypeName = "line"; break;

    case NAME: tokenTypeValue = 10; tokenTypeName = "identifier"; break;
    case FIELD: tokenTypeValue = 11; tokenTypeName = "field"; break;
    case OPERATOR: tokenTypeValue = 12; tokenTypeName = "operator"; break;

    case BOOL: tokenTypeValue = 13; tokenTypeName = "boolLiteral"; break;
    case INT: tokenTypeValue = 14; tokenTypeName = "intLiteral"; break;
    case STRING: tokenTypeValue = 15; tokenTypeName = "stringLiteral"; break;

    case EOF: tokenTypeValue = 16; tokenTypeName = "eof"; break;

    default:
      // TODO(bob): Better error reporting.
      mInterpreter.throwError("ParseError");
      tokenTypeValue = -1;    
      tokenTypeName = "";
    }
    
    return construct("TokenType",
        "name", tokenTypeName,
        "value", tokenTypeValue);    
  }
  
  private Obj convertObject(Object object) {
    if (object == null) return mInterpreter.nothing();
    if (object instanceof Obj) return (Obj) object;
    if (object instanceof Boolean) return mInterpreter.createBool((Boolean) object);
    if (object instanceof Integer) return mInterpreter.createInt((Integer) object);
    if (object instanceof String) return mInterpreter.createString((String) object);
    if (object instanceof Expr) return convertExpr((Expr) object);
    if (object instanceof Pattern) return convertPattern((Pattern) object);
    if (object instanceof Position) return convertPosition((Position) object);
    if (object instanceof Token) return convertToken((Token) object);
    if (object instanceof TokenType) return convertTokenType((TokenType) object);
    
    throw new UnsupportedOperationException("Don't know how to convert an " +
        "object of type " + object.getClass().getSimpleName() + ".");
  }
  
  private Obj convertList(List<?> values) {
    List<Obj> objs = new ArrayList<Obj>();
    for (Object value : values) {
      objs.add(convertObject(value));
    }
    return mInterpreter.createArray(objs);
  }
  
  private JavaToMagpie(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }

  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
