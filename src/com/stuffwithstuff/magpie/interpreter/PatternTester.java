package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.pattern.*;

/**
 * Determines if a pattern matches a given value.
 */
public class PatternTester implements PatternVisitor<Boolean, Obj> {
  public static boolean test(final Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    
    PatternTester binder = new PatternTester(interpreter, context);
    return pattern.accept(binder, value);
  }
  
  @Override
  public Boolean visit(RecordPattern pattern, Obj value) {
    // Test each field.
    for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
      Obj fieldValue = value.getField(field.getKey());
      if (fieldValue == null) return false;
      if (!field.getValue().accept(this, fieldValue)) return false;
    }
    
    // If we got here, the fields all passed.
    return true;
  }
  
  @Override
  public Boolean visit(TypePattern pattern, Obj value) {
    Obj expected = mInterpreter.evaluate(pattern.getType(), mContext);
    return value.getClassObj().isSubclassOf((ClassObj)expected);
  }
  
  @Override
  public Boolean visit(ValuePattern pattern, Obj value) {
    Obj expected = mInterpreter.evaluate(pattern.getValue(), mContext);
    return mInterpreter.objectsEqual(expected, value);
  }

  @Override
  public Boolean visit(VariablePattern pattern, Obj value) {
    return pattern.getPattern().accept(this, value);
  }

  @Override
  public Boolean visit(WildcardPattern pattern, Obj value) {
    return true;
  }

  private PatternTester(Interpreter interpreter, EvalContext context) {
    mInterpreter = interpreter;
    mContext = context;
  }
  
  private final Interpreter mInterpreter;
  private final EvalContext mContext;
}
