package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Handles binding the variables in a pattern during type-checking.
 */
public class PatternCheckingBinder extends PatternBinderBase {
  public static void bind(final Checker checker, final Position position,
      Pattern pattern, Obj value, EvalContext context) {
    PatternCheckingBinder binder = new PatternCheckingBinder(
        checker, position, context);
    pattern.accept(binder, value);
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // Make sure the type of the pattern's value is compatible with the type
    // being matched.
    Obj matchedType = mChecker.evaluateExpressionType(pattern.getValue());
    
    mChecker.checkTypes(value, matchedType, Position.none(),
        "Cannot match a value of type %s against a pattern of type %s.");

    return null;
  }

  @Override
  public Void visit(VariablePattern pattern, Obj value) {
    // Type-check if we have a type.
    if (pattern.getType() != null) {
      // Make sure the type of the pattern's value is compatible with the type
      // being matched.
      Obj matchedType = getInterpreter().evaluate(pattern.getType());
      mChecker.checkTypes(value, matchedType, Position.none(),
      "Cannot match a value of type %s against type %s.");
      
      // Cast the variable to the matched type.
      value = matchedType;
    }
    
    // Bind the variable.
    if (!bindNewVariable(pattern.getName(), value)) {
      // Cannot redefine a variable in the same scope.
      mChecker.addError(mPosition,
          "There is already a variable named \"%s\" declared in this scope.",
          pattern.getName());
    }

    return null;
  }

  private PatternCheckingBinder(Checker checker, Position position,
      EvalContext context) {
    super(checker.getInterpreter(), context);

    mChecker = checker;
    mPosition = position;
  }
  
  private final Checker mChecker;
  private final Position mPosition;
}
