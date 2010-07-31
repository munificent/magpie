package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import com.stuffwithstuff.magpie.ast.Expr;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj {
  public FnObj(Obj parent, List<String> paramNames, Expr body) {
    super(parent);
    
    mParamNames = paramNames;
    mBody = body;
  }

  public List<String> getParamNames() { return mParamNames; }
  public Expr getBody() { return mBody; }
  
  private final List<String> mParamNames;
  private final Expr mBody;
}
