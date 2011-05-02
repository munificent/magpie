package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.AssignExpr;
import com.stuffwithstuff.magpie.ast.BoolExpr;
import com.stuffwithstuff.magpie.ast.BreakExpr;
import com.stuffwithstuff.magpie.ast.CallExpr;
import com.stuffwithstuff.magpie.ast.ClassExpr;
import com.stuffwithstuff.magpie.ast.DefineExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ExprVisitor;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.ImportExpr;
import com.stuffwithstuff.magpie.ast.IntExpr;
import com.stuffwithstuff.magpie.ast.ListExpr;
import com.stuffwithstuff.magpie.ast.LoopExpr;
import com.stuffwithstuff.magpie.ast.MatchExpr;
import com.stuffwithstuff.magpie.ast.MethodExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.RecordExpr;
import com.stuffwithstuff.magpie.ast.ReturnExpr;
import com.stuffwithstuff.magpie.ast.ScopeExpr;
import com.stuffwithstuff.magpie.ast.SequenceExpr;
import com.stuffwithstuff.magpie.ast.StringExpr;
import com.stuffwithstuff.magpie.ast.ThrowExpr;
import com.stuffwithstuff.magpie.ast.VariableExpr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.PatternVisitor;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.ast.pattern.WildcardPattern;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;
import com.stuffwithstuff.magpie.util.NotImplementedException;
import com.stuffwithstuff.magpie.util.Pair;

public class JavaToMagpie {
  public static Obj convert(Interpreter interpreter, Expr expr) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, null);
    return javaToMagpie.convertExpr(expr);
  }
  
  public static Obj convertAndUnquote(Interpreter interpreter, Expr expr,
      EvalContext context) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, context);
    return javaToMagpie.convertExpr(expr);
  }
  
  public static Obj convert(Interpreter interpreter, Pattern pattern) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, null);
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
  
  private JavaToMagpie(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }

  private Obj convertExpr(Expr expr) {
    if (expr == null) return mInterpreter.nothing();
    ExprConverter converter = new ExprConverter();
    return expr.accept(converter, null);
  }
  
  private Obj convertList(List<?> values) {
    List<Obj> objs = new ArrayList<Obj>();
    for (Object value : values) {
      objs.add(convertObject(value));
    }
    return mInterpreter.createList(objs);
  }
  
  private Obj convertMatchCase(MatchCase matchCase) {
    if (matchCase == null) return mInterpreter.nothing();
    
    return construct("MatchCase",
        "pattern", matchCase.getPattern(),
        "body",    matchCase.getBody());
  }

  private Obj convertPattern(Pattern pattern) {
    if (pattern == null) return mInterpreter.nothing();
    
    PatternConverter converter = new PatternConverter();
    return pattern.accept(converter, null);
  }
  
  private Obj convertPosition(Position position) {
    return construct("Position",
        "file",      position.getSourceFile(),
        "startLine", position.getStartLine(),
        "startCol",  position.getStartCol(),
        "endLine",   position.getEndLine(),
        "endCol",    position.getEndCol());
  }
  
  private Obj convertToken(Token token) {
    if (token == null) return mInterpreter.nothing();
    
    return construct("Token",
        "position",  token.getPosition(),
        "type",      token.getType(),
        "text",      token.getText(),
        "value",     token.getValue());
  }
  
  private Obj convertTokenType(TokenType type) {
    return construct("TokenType",
        "name", type.toString());    
  }
  
  private Obj convertObject(Object object) {
    if (object == null) return mInterpreter.nothing();
    if (object instanceof Obj) return (Obj) object;
    if (object instanceof Boolean) return mInterpreter.createBool((Boolean) object);
    if (object instanceof Integer) return mInterpreter.createInt((Integer) object);
    if (object instanceof String) return mInterpreter.createString((String) object);
    if (object instanceof Expr) return convertExpr((Expr) object);
    if (object instanceof MatchCase) return convertMatchCase((MatchCase) object);
    if (object instanceof Pattern) return convertPattern((Pattern) object);
    if (object instanceof Position) return convertPosition((Position) object);
    if (object instanceof Token) return convertToken((Token) object);
    if (object instanceof TokenType) return convertTokenType((TokenType) object);
    
    throw new UnsupportedOperationException("Don't know how to convert an " +
        "object of type " + object.getClass().getSimpleName() + ".");
  }

  private Obj construct(String className, Object... args) {
    ClassObj classObj = mInterpreter.getSyntaxModule().getExportedVariables()
        .get(className).asClass();
    Obj object = mInterpreter.instantiate(classObj, null);

    // TODO(bob): Hackish. Goes around normal object construction process.
    for (int i = 0; i < args.length; i += 2) {
      object.setField((String) args[i], convertObject(args[i + 1]));
    }
    
    return object;
  }

  private class ExprConverter implements ExprVisitor<Obj, Void> {
    @Override
    public Obj visit(AssignExpr expr, Void context) {
      return construct("AssignExpression",
          "position", expr.getPosition(),
          "name",     expr.getName(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(BoolExpr expr, Void context) {
      return construct("BoolExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(BreakExpr expr, Void context) {
      return construct("BreakExpression",
          "position", expr.getPosition());
    }

    @Override
    public Obj visit(CallExpr expr, Void context) {
      return construct("CallExpression",
          "position", expr.getPosition(),
          "receiver", expr.getReceiver(),
          "name",     expr.getName(),
          "argument", expr.getArg());
    }

    @Override
    public Obj visit(ClassExpr expr, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      
      for (Entry<String, Field> field : expr.getFields().entrySet()) {
        fields.add(construct("Field",
            "mutable?", field.getValue().isMutable(),
            "initializer", field.getValue().getInitializer(),
            "type",        field.getValue().getType()));
      }
      
      return construct("ClassExpression",
          "position", expr.getPosition(),
          "doc",      expr.getDoc(),
          "name",     expr.getName(),
          "parents",  convertList(expr.getParents()),
          "fields",   convertList(fields));
    }

    @Override
    public Obj visit(DefineExpr expr, Void context) {
      return construct("DefineExpression",
          "position", expr.getPosition(),
          "pattern",  expr.getPattern(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(FnExpr expr, Void context) {
      return construct("FunctionExpression",
          "position", expr.getPosition(),
          "doc",      expr.getDoc(),
          "pattern",  expr.getPattern(),
          "body",     expr.getBody());
    }

    @Override
    public Obj visit(ImportExpr expr, Void context) {
      return construct("ImportExpression",
          "position", expr.getPosition(),
          "scheme",   expr.getScheme(),
          "module",   expr.getModule(),
          "name",     expr.getName(),
          "rename",   expr.getRename());
    }

    @Override
    public Obj visit(IntExpr expr, Void context) {
      return construct("IntExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(ListExpr expr, Void context) {
      return construct("ListExpression",
          "position", expr.getPosition(),
          "elements", convertList(expr.getElements()));
    }

    @Override
    public Obj visit(LoopExpr expr, Void context) {
      return construct("LoopExpression",
          "position", expr.getPosition(),
          "body",     expr.getBody());
    }

    @Override
    public Obj visit(MatchExpr expr, Void context) {
      return construct("MatchExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue(),
          "cases",    convertList(expr.getCases()));
    }

    @Override
    public Obj visit(VariableExpr expr, Void context) {
      return construct("VariableExpression",
          "position", expr.getPosition(),
          "name",     expr.getName());
    }

    @Override
    public Obj visit(MethodExpr expr, Void context) {
      return construct("MethodExpression",
          "position", expr.getPosition(),
          "doc",      expr.getDoc(),
          "name",     expr.getName(),
          "pattern",  expr.getPattern(),
          "body",     expr.getBody());
    }

    @Override
    public Obj visit(NothingExpr expr, Void context) {
      return construct("NothingExpression",
          "position", expr.getPosition());
    }

    @Override
    public Obj visit(RecordExpr expr, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Pair<String, Expr> field : expr.getFields()) {
        Obj name = mInterpreter.createString(field.getKey());
        Obj value = convertObject(field.getValue());
        fields.add(mInterpreter.createRecord(name, value));
      }

      return construct("RecordExpression",
          "position", expr.getPosition(),
          "fields",   mInterpreter.createList(fields));
    }

    @Override
    public Obj visit(ReturnExpr expr, Void context) {
      return construct("ReturnExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(ScopeExpr expr, Void context) {
      return construct("ScopeExpression",
          "position", expr.getPosition(),
          "body",     expr.getBody(),
          "catches",  convertList(expr.getCatches()));
    }

    @Override
    public Obj visit(SequenceExpr expr, Void context) {
      return construct("SequenceExpression",
          "position",    expr.getPosition(),
          "expressions", convertList(expr.getExpressions()));
    }

    @Override
    public Obj visit(StringExpr expr, Void context) {
      return construct("StringExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
    }

    @Override
    public Obj visit(ThrowExpr expr, Void context) {
      return construct("ThrowExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
    }
  }
  
  public class PatternConverter implements PatternVisitor<Obj, Void> {

    @Override
    public Obj visit(RecordPattern pattern, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
        Obj name = mInterpreter.createString(field.getKey());
        Obj value = convertObject(field.getValue());
        fields.add(mInterpreter.createRecord(name, value));
      }

      return construct("RecordPattern",
          "fields", mInterpreter.createList(fields));
    }

    @Override
    public Obj visit(TypePattern pattern, Void context) {
      return construct("TypePattern",
          "type", pattern.getType());
    }

    @Override
    public Obj visit(ValuePattern pattern, Void context) {
      return construct("ValuePattern",
          "value", pattern.getValue());
    }

    @Override
    public Obj visit(VariablePattern pattern, Void context) {
      return construct("VariablePattern",
          "name",    pattern.getName(),
          "pattern", pattern.getPattern());
    }

    @Override
    public Obj visit(WildcardPattern pattern, Void context) {
      return construct("WildcardPattern");
    }
  }
  
  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
