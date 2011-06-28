package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.AssignExpr;
import com.stuffwithstuff.magpie.ast.BoolExpr;
import com.stuffwithstuff.magpie.ast.BreakExpr;
import com.stuffwithstuff.magpie.ast.CallExpr;
import com.stuffwithstuff.magpie.ast.ClassExpr;
import com.stuffwithstuff.magpie.ast.ImportDeclaration;
import com.stuffwithstuff.magpie.ast.QuoteExpr;
import com.stuffwithstuff.magpie.ast.UnquoteExpr;
import com.stuffwithstuff.magpie.ast.VarExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ExprVisitor;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.ImportExpr;
import com.stuffwithstuff.magpie.ast.IntExpr;
import com.stuffwithstuff.magpie.ast.ArrayExpr;
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
import com.stuffwithstuff.magpie.ast.NameExpr;
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
import com.stuffwithstuff.magpie.util.Pair;

public class JavaToMagpie {
  public static Obj convert(Context context, Expr expr) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(context, null);
    return javaToMagpie.convertExpr(expr);
  }
  
  public static Obj convertAndUnquote(Context context, Expr expr,
      Scope scope) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(context, scope);
    return javaToMagpie.convertExpr(expr);
  }
  
  public static Obj convert(Context context, Pattern pattern) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(context, null);
    return javaToMagpie.convertPattern(pattern);
  }

  public static Obj convert(Context context, TokenType type) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(context, null);
    return javaToMagpie.convertTokenType(type);
  }
  
  public static Obj convert(Context context, Token token) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(context, null);
    return javaToMagpie.convertToken(token);
  }
  
  private JavaToMagpie(Context context, Scope scope) {
    mContext = context;
    mScope = scope;
  }

  private Obj convertExpr(Expr expr) {
    if (expr == null) return mContext.nothing();
    ExprConverter converter = new ExprConverter();
    return expr.accept(converter, null);
  }
  
  private Obj convertArray(List<?> values) {
    List<Obj> objs = new ArrayList<Obj>();
    for (Object value : values) {
      objs.add(convertObject(value));
    }
    return mContext.toArray(objs);
  }
  
  private Obj convertMatchCase(MatchCase matchCase) {
    if (matchCase == null) return mContext.nothing();
    
    return construct("MatchCase",
        "pattern", matchCase.getPattern(),
        "body",    matchCase.getBody());
  }

  private Obj convertPattern(Pattern pattern) {
    if (pattern == null) return mContext.nothing();
    
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
    if (token == null) return mContext.nothing();
    
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
    if (object == null) return mContext.nothing();
    if (object instanceof Obj) return (Obj) object;
    if (object instanceof Boolean) return mContext.toObj((Boolean) object);
    if (object instanceof Integer) return mContext.toObj((Integer) object);
    if (object instanceof String) return mContext.toObj((String) object);
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
    ClassObj classObj = mContext.getInterpreter().getSyntaxModule().getExportedVariable(className).asClass();
    Obj object = mContext.instantiate(classObj, null);

    // TODO(bob): Hackish. Goes around normal object construction process.
    for (int i = 0; i < args.length; i += 2) {
      object.setField((String) args[i], convertObject(args[i + 1]));
    }
    
    return object;
  }

  private class ExprConverter implements ExprVisitor<Obj, Void> {
    @Override
    public Obj visit(ArrayExpr expr, Void context) {
      return construct("ArrayExpression",
          "position", expr.getPosition(),
          "elements", convertArray(expr.getElements()));
    }

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
          "name",     expr.getName(),
          "argument", expr.getArg());
    }

    @Override
    public Obj visit(ClassExpr expr, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      
      for (Entry<String, Field> field : expr.getFields().entrySet()) {
        fields.add(construct("Field",
            "isMutable",   field.getValue().isMutable(),
            "initializer", field.getValue().getInitializer(),
            "pattern",     field.getValue().getPattern()));
      }
      
      return construct("ClassExpression",
          "position", expr.getPosition(),
          "doc",      expr.getDoc(),
          "name",     expr.getName(),
          "parents",  convertArray(expr.getParents()),
          "fields",   convertArray(fields));
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
      List<Obj> declarations = new ArrayList<Obj>();
      
      for (ImportDeclaration declaration : expr.getDeclarations()) {
        declarations.add(construct("ImportDeclaration",
            "isExported", declaration.isExported(),
            "name",       declaration.getName(),
            "rename",     declaration.getRename()));
      }
      
      return construct("ImportExpression",
          "position",     expr.getPosition(),
          "scheme",       expr.getScheme(),
          "module",       expr.getModule(),
          "prefix",       expr.getPrefix(),
          "isOnly",       expr.isOnly(),
          "declarations", convertArray(declarations));
    }

    @Override
    public Obj visit(IntExpr expr, Void context) {
      return construct("IntExpression",
          "position", expr.getPosition(),
          "value",    expr.getValue());
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
          "cases",    convertArray(expr.getCases()));
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
    public Obj visit(NameExpr expr, Void context) {
      return construct("NameExpression",
          "position", expr.getPosition(),
          "name",     expr.getName());
    }

    @Override
    public Obj visit(NothingExpr expr, Void context) {
      return construct("NothingExpression",
          "position", expr.getPosition());
    }

    @Override
    public Obj visit(QuoteExpr expr, Void context) {
      return construct("QuoteExpression",
          "position", expr.getPosition(),
          "body",     expr.getBody());
    }

    @Override
    public Obj visit(RecordExpr expr, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Pair<String, Expr> field : expr.getFields()) {
        Obj name = mContext.toObj(field.getKey());
        Obj value = convertObject(field.getValue());
        fields.add(mContext.toObj(name, value));
      }

      return construct("RecordExpression",
          "position", expr.getPosition(),
          "fields",   mContext.toList(fields));
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
          "catches",  convertArray(expr.getCatches()));
    }

    @Override
    public Obj visit(SequenceExpr expr, Void context) {
      return construct("SequenceExpression",
          "position",    expr.getPosition(),
          "expressions", convertArray(expr.getExpressions()));
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

    @Override
    public Obj visit(UnquoteExpr expr, Void context) {
      // If we have a Scope, then we're converting a quotation and we
      // should evaluate the unquote.
      if (mScope != null) {
        // TODO(bob): Check that it evaluates to an expression?
        Obj value = mContext.evaluate(expr.getBody(), mScope);
        
        // If the unquoted value is a primitive object, automatically promote
        // it to a corresponding literal.
        if (mContext.isBool(value)) {
          value = construct("BoolExpression",
              "position", expr.getPosition(),
              "value",    value);
        } else if (mContext.isInt(value)) {
          value = construct("IntExpression",
              "position", expr.getPosition(),
              "value",    value);
        } else if (mContext.isString(value)) {
          value = construct("StringExpression",
              "position", expr.getPosition(),
              "value",    value);
        } else if (mContext.isNothing(value)) {
          value = construct("NothingExpression",
              "position", expr.getPosition());
        }
        return value;
      } else {
        // Just a straight conversion.
        return construct("UnquoteExpression",
            "position", expr.getPosition(),
            "value",    expr.getBody());
      }
    }

    @Override
    public Obj visit(VarExpr expr, Void context) {
      return construct("VarExpression",
          "position",  expr.getPosition(),
          "isMutable", expr.isMutable(),
          "pattern",   expr.getPattern(),
          "value",     expr.getValue());
    }
  }
  
  public class PatternConverter implements PatternVisitor<Obj, Void> {

    @Override
    public Obj visit(RecordPattern pattern, Void context) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
        Obj name = mContext.toObj(field.getKey());
        Obj value = convertObject(field.getValue());
        fields.add(mContext.toObj(name, value));
      }

      return construct("RecordPattern",
          "fields", mContext.toList(fields));
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
  
  private final Context mContext;
  private final Scope mScope;
}
