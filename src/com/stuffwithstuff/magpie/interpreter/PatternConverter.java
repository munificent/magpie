package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.PatternVisitor;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.ast.pattern.WildcardPattern;
import com.stuffwithstuff.magpie.util.NotImplementedException;

public class PatternConverter implements PatternVisitor<Obj, EvalContext> {
  public static Pattern convert(Interpreter interpreter, Obj pattern) {
    if (pattern == interpreter.nothing()) return null;
    
    ClassObj patternClass = pattern.getClassObj();
    if (patternClass == interpreter.getGlobal("TuplePattern")) {
      return convertTuplePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("TypePattern")) {
      return convertTypePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("ValuePattern")) {
      return convertValuePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("VariablePattern")) {
      return convertVariablePattern(interpreter, pattern);
    } else if (patternClass == interpreter.getGlobal("WildcardPattern")) {
      return new WildcardPattern();
    }
    
    // TODO(bob): Add better error-handling.
    throw new NotImplementedException(
        "Other pattern types not implemented yet!");
  }
  
  private static Pattern convertTuplePattern(Interpreter interpreter,
      Obj pattern) {
    List<Obj> fieldObjs = pattern.getField("fields").asArray();
    List<Pattern> fields = new ArrayList<Pattern>();
    for (Obj field : fieldObjs) {
      fields.add(convert(interpreter, field));
    }
    
    return new TuplePattern(fields);
  }
  
  private static Pattern convertTypePattern(Interpreter interpreter,
      Obj pattern) {
    Obj type = pattern.getField("typeExpr");
    Expr expr = ExprConverter.convert(interpreter, type);
    return new TypePattern(expr);
  }
  
  private static Pattern convertValuePattern(Interpreter interpreter,
      Obj pattern) {
    Obj value = pattern.getField("value");
    Expr expr = ExprConverter.convert(interpreter, value);
    return new ValuePattern(expr);
  }
  
  private static Pattern convertVariablePattern(Interpreter interpreter,
      Obj pattern) {
    String name = pattern.getField("name").asString();
    Obj patternObj = pattern.getField("pattern");
    Pattern innerPattern = null;
    if (patternObj != interpreter.nothing()) {
      innerPattern = convert(interpreter, patternObj);
    }
    return new VariablePattern(name, innerPattern);
  }
  
  public static Obj convert(Pattern pattern, Interpreter interpreter,
      EvalContext context) {
    PatternConverter converter = new PatternConverter(interpreter);
    return converter.convert(pattern, context);
  }
  
  @Override
  public Obj visit(TuplePattern pattern, EvalContext context) {
    List<Obj> fields = new ArrayList<Obj>();
    for (Pattern field : pattern.getFields()) {
      fields.add(convert(field, context));
    }
    Obj fieldsArray = mInterpreter.createArray(fields);
    
    return mInterpreter.construct("TuplePattern", fieldsArray);  }

  @Override
  public Obj visit(TypePattern pattern, EvalContext context) {
    Obj type = ExprConverter.convert(mInterpreter, pattern.getType(), context);
    return mInterpreter.construct("TypePattern", type);
  }

  @Override
  public Obj visit(ValuePattern pattern, EvalContext context) {
    Obj value = ExprConverter.convert(mInterpreter, pattern.getValue(), context);
    return mInterpreter.construct("ValuePattern", value);
  }

  @Override
  public Obj visit(VariablePattern pattern, EvalContext context) {
    Obj name = mInterpreter.createString(pattern.getName());
    Obj innerPattern = convert(pattern.getPattern(), context);
    return mInterpreter.construct("VariablePattern", name, innerPattern);
  }

  @Override
  public Obj visit(WildcardPattern pattern, EvalContext context) {
    return mInterpreter.construct("WildcardPattern");
  }
  
  private Obj convert(Pattern pattern, EvalContext context) {
    if (pattern == null) return mInterpreter.nothing();
    
    return pattern.accept(this, context);
  }
  
  private PatternConverter(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  private Interpreter mInterpreter;
}
