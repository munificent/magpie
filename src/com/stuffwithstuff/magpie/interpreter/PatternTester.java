package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Determines if a pattern matches a given value.
 * 
 * @author bob
 */
public class PatternTester implements PatternVisitor<Boolean, Obj> {
  public static boolean test(final Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    
    PatternTester binder = new PatternTester(interpreter, context);
    return pattern.accept(binder, value);
  }

  @Override
  public Boolean visit(TuplePattern pattern, Obj value) {
    // Test each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = mInterpreter.getMember(Position.none(), value, "_" + i);
      if (!fieldPattern.accept(this, field)) return false;
    }
    
    // If we got here, the fields all passed.
    return true;
  }

  @Override
  public Boolean visit(TypePattern pattern, Obj value) {
    // TODO(bob): Should this be evaluated in the regular context even though
    // it's a type?
    Obj expected = mInterpreter.evaluate(pattern.getType(), mContext);
    Obj result = mInterpreter.invokeMethod(value, "is", expected);
    
    return result.asBool();
  }

  @Override
  public Boolean visit(ValuePattern pattern, Obj value) {
    Obj expected = mInterpreter.evaluate(pattern.getValue(), mContext);
    
    Obj equals = mContext.lookUp(Name.EQEQ);
    Obj result = mInterpreter.apply(Position.none(), equals,
        mInterpreter.createTuple(value, expected));
    
    return result.asBool();
  }

  @Override
  public Boolean visit(VariablePattern pattern, Obj value) {
    // If we have a pattern for the variable, recurse into it.
    if (pattern.getPattern() != null) {
      return pattern.getPattern().accept(this, value);
    }
    
    // A variable matches anything.
    return true;
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
