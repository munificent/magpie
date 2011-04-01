package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class MatchCase {
  public MatchCase(Expr body) {
    this(Pattern.wildcard(), body);
  }
  
  public MatchCase(Pattern pattern, Expr body) {
    mPattern = pattern;
    mBody = body;
  }
  
  public Pattern getPattern() { return mPattern; }
  public Expr    getBody()    { return mBody; }
  
  private final Pattern mPattern;
  private final Expr mBody;
}