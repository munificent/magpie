package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Given a pattern, a value, and a context, destructures the value and binds
 * new variables in the context.
 */
public class PatternBinder implements PatternVisitor<Void, Obj>  {
  public static void bind(Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    PatternBinder binder = new PatternBinder(interpreter, context);
    pattern.accept(binder, value);
  }

  @Override
  public Void visit(RecordPattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pair<String, Pattern> field = pattern.getFields().get(i);
      Obj fieldValue = value.getField(field.getKey());
      field.getValue().accept(this, fieldValue);
    }
    
    return null;
  }
  
  @Override
  public Void visit(TuplePattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = value.getTupleField(i);
      fieldPattern.accept(this, field);
    }
    
    return null;
  }

  @Override
  public Void visit(TypePattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visit(VariablePattern pattern, Obj value) {
    // Bind the variable.
    if (!bindNewVariable(pattern.getName(), value)) {
      // Cannot redefine a variable in the same scope.
      mInterpreter.error("RedefinitionError",
          String.format("There is already a variable named \"%s\" in this scope.", pattern.getName()));
    }
    
    // Recurse into the inner pattern if there is one.
    if (pattern.getPattern() != null) {
      pattern.getPattern().accept(this, value);
    }

    return null;
  }

  @Override
  public Void visit(WildcardPattern pattern, Obj value) {
    // Do nothing.
    return null;
  }

  private PatternBinder(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  private boolean bindNewVariable(String name, Obj value) {
    // Ignore the wildcard name.
    if (name.equals("_")) return true;
    
    // Bind the variable.
    return mContext.define(name, value);
  }
  
  private final Interpreter mInterpreter;  
  private final EvalContext mContext;
}
