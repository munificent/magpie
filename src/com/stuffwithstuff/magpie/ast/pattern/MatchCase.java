package com.stuffwithstuff.magpie.ast.pattern;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;

public class MatchCase {
  public MatchCase(String binding, Pattern pattern, Expr body) {
    mBinding = binding;
    mPattern = pattern;
    mBody = body;
  }
  
  public boolean hasBinding() { return mBinding != null; }
  public String getBinding() { return mBinding; }
  public Pattern getPattern() { return mPattern; }
  public Expr getBody() { return mBody; }
  
  public final String mBinding;
  public final Pattern mPattern;
  public final Expr mBody;
}