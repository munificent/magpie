package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.interpreter.PatternTyper;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Describes a function's type declaration, including its parameter and return
 * type, along with its parameter names, if any.
 */
public class FunctionType {
  public static FunctionType nothingToDynamic() {
    return returningType(Expr.name("Dynamic"));
  }
  
  public static FunctionType returningType(Expr type) {
    return new FunctionType(new ValuePattern(Expr.nothing()), type, false);
  }
  
  public FunctionType(Pattern pattern, Expr returnType, boolean isStatic) {
    Expect.notNull(pattern);
    Expect.notNull(returnType);
    
    mReturnType = returnType;
    mPattern = pattern;
    mIsStatic = isStatic;
  }
  
  public Pattern      getPattern()      { return mPattern; }
  public Expr         getReturnType()   { return mReturnType; }
  public boolean      isStatic()        { return mIsStatic; }

  public Expr getParamType() {
    // Evaluate the static type of the pattern.
    return PatternTyper.evaluate(mPattern);
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
    builder.append("(").append(mPattern);
    builder.append(" -> ").append(mReturnType).append(")");
    
    return builder.toString();
  }
  
  private final Pattern mPattern;
  private final Expr    mReturnType;
  private final boolean mIsStatic;
  
}
