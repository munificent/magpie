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
import com.stuffwithstuff.magpie.util.Pair;

public class JavaToMagpie {
  public static Obj convert(Interpreter interpreter, Expr expr,
      EvalContext context) {
    
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, context);
    return javaToMagpie.convert(expr);
  }
  
  public static Obj convert(Interpreter interpreter, Pattern pattern,
      EvalContext context) {
    JavaToMagpie javaToMagpie = new JavaToMagpie(interpreter, context);
    return javaToMagpie.convert(pattern);
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
  }
  
  public class PatternConverter implements PatternVisitor<Obj, Void> {
    @Override
    public Obj visit(RecordPattern pattern, Void dummy) {
      List<Obj> fields = new ArrayList<Obj>();
      for (Pair<String, Pattern> field : pattern.getFields()) {
        Obj name = mInterpreter.createString(field.getKey());
        Obj value = convert(field.getValue());
        fields.add(mInterpreter.createTuple(name, value));
      }

      return construct("RecordPattern",
          "fields", mInterpreter.createArray(fields));
    }

    @Override
    public Obj visit(TuplePattern pattern, Void dummy) {
      return construct("TuplePattern",
          "fields", convert(pattern.getFields()));
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
        value = convert((Pattern) rawValue);
      } else {
        value = (Obj) rawValue;
      }
      
      fields.put(name, value);
    }
    
    return mInterpreter.invokeMethod(mInterpreter.getGlobal(className),
        "construct", mInterpreter.createRecord(fields));
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
    ExprConverter converter = new ExprConverter();
    return expr.accept(converter, null);
  }

  private Obj convert(Pattern pattern) {
    if (pattern == null) return mInterpreter.nothing();
    
    PatternConverter converter = new PatternConverter();
    return pattern.accept(converter, null);
  }
  
  private Obj convert(Object object) {
    if (object == null) return mInterpreter.nothing();
    if (object instanceof Expr) return convert((Expr) object);
    if (object instanceof Pattern) return convert((Pattern) object);
    
    throw new UnsupportedOperationException("Don't know how to convert an " +
        "object of type " + object.getClass().getSimpleName() + ".");
  }
  
  private Obj convert(List<?> values) {
    List<Obj> objs = new ArrayList<Obj>();
    for (Object value : values) {
      objs.add(convert(value));
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
