package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Given a pattern, a value, and a context, destructures the value and binds
 * new variables in a new child context.
 * 
 * @author bob
 */
public class PatternBinder implements PatternVisitor<Void, Obj> {
  public static EvalContext bind(Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    context = context.pushScope();
    PatternBinder binder = new PatternBinder(interpreter, context);
    pattern.accept(binder, value);
    return context;
  }
  
  @Override
  public Void visit(TuplePattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = mInterpreter.getMember(Position.none(), value, "_" + i);
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
    // If we have a pattern for the variable, recurse into it.
    if (pattern.getPattern() != null) {
      pattern.getPattern().accept(this, value);
    }
    
    // Bind the variable.
    // TODO(bob): What happens if the name is already in use?
    mContext.define(pattern.getName(), value);
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
  
  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
