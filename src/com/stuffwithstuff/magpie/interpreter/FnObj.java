package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * Object type for a function object.
 */
public class FnObj extends Obj {
  public FnObj(Obj parent, FnExpr function) {
    super(parent);
    
    mFunction = function;
  }

  public FnExpr getFunction() { return mFunction; }
  
  private final FnExpr mFunction;
}
