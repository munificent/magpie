package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Base class for pattern visitors that bind variables from a pattern.
 */
public abstract class PatternBinderBase implements PatternVisitor<Void, Obj> {
  @Override
  public Void visit(RecordPattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pair<String, Pattern> field = pattern.getFields().get(i);
      Obj fieldValue = mInterpreter.getQualifiedMember(
          Position.none(), value, field.getKey());
      field.getValue().accept(this, fieldValue);
    }
    
    return null;
  }
  
  @Override
  public Void visit(TuplePattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = mInterpreter.getQualifiedMember(
          Position.none(), value, "_" + i);
      fieldPattern.accept(this, field);
    }
    
    return null;
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  @Override
  public abstract Void visit(VariablePattern pattern, Obj value);
  
  protected PatternBinderBase(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  protected Interpreter getInterpreter() { return mInterpreter; }
  
  protected boolean bindNewVariable(String name, Obj value) {
    // Bind the variable.
    if (mContext.lookUpHere(name) == null) {
      // Ignore the wildcard name.
      if (!name.equals("_")) {
        mContext.define(name, value);
      }
      
      return true;
    }
    
    return false;
  }
  
  private final Interpreter mInterpreter;  
  private final EvalContext mContext;
}
