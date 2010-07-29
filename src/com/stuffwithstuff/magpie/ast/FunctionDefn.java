package com.stuffwithstuff.magpie.ast;

import java.util.List;

import com.stuffwithstuff.magpie.type.*;

/**
 * AST node for a function definition.
 */
public class FunctionDefn {
  public FunctionDefn(String name, FunctionType type, List<String> paramNames, Expr body) {
    mName = name;
    mType = type;
    mParamNames = paramNames;
    mBody = body;
  }
  
  public String getName() { return mName; }
  public FunctionType getType() { return mType; }
  public List<String> getParamNames() { return mParamNames; }
  public Expr   getBody() { return mBody; }
  
  private final String mName;
  private final FunctionType mType;
  private final List<String> mParamNames;
  private final Expr   mBody;
}
