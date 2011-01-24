package com.stuffwithstuff.magpie.ast;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.interpreter.PatternTyper;
import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Describes a function's type declaration, including its parameter and return
 * type, along with its parameter names, if any.
 */
public class FunctionType {
  public static FunctionType nothingToDynamic() {
    return returningType(Expr.name("Dynamic"));
  }
  
  public static FunctionType returningType(Expr type) {
    return new FunctionType(new ValuePattern(Expr.nothing()), type);
  }
  
  public FunctionType(Pattern pattern, Expr returnType) {
    this(new ArrayList<Pair<String, Expr>>(), pattern, returnType);
  }
  
  public FunctionType(List<Pair<String, Expr>> typeParams, Pattern pattern,
      Expr returnType) {
    Expect.notNull(typeParams);
    Expect.notNull(pattern);
    Expect.notNull(returnType);
    
    mTypeParams = typeParams;
    mReturnType = returnType;
    mPattern = pattern;
  }
  
  public List<Pair<String, Expr>> getTypeParams() { return mTypeParams; }
  public Pattern      getPattern()      { return mPattern; }
  public Expr         getReturnType()   { return mReturnType; }

  public Expr getParamType() {
    // Evaluate the static type of the pattern.
    return PatternTyper.evaluate(mPattern);
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
    if (mTypeParams.size() > 0) {
      builder.append("[");
      boolean first = true;
      for (Pair<String, Expr> typeParam : mTypeParams) {
        if (!first) builder.append(" ");
        builder.append(typeParam.getKey());
        String constraint = typeParam.getValue().toString();
        if (!constraint.equals("Any")) {
          builder.append(" ").append(constraint);
        }
        first = false;
      }
      builder.append("]");
    }
    
    builder.append("(").append(mPattern);
    builder.append(" -> ").append(mReturnType).append(")");
    
    return builder.toString();
  }
  
  private final List<Pair<String, Expr>> mTypeParams;
  private final Pattern           mPattern;
  private final Expr              mReturnType;
}
