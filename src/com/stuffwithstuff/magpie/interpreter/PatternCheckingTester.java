package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Checks if a pattern can possibly match a value of the given type.
 */
// TODO(bob): There's overlap here with PatternCheckingBinder. Should unify.
public class PatternCheckingTester implements PatternVisitor<Boolean, Obj> {
  public static boolean test(Checker checker, Pattern pattern, Obj type) {
    PatternCheckingTester tester = new PatternCheckingTester(checker);
    return pattern.accept(tester, type);
  }
  
  @Override
  public Boolean visit(RecordPattern pattern, Obj type) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pair<String, Pattern> field = pattern.getFields().get(i);
      Obj fieldValue = mChecker.getInterpreter().getQualifiedMember(
          Position.none(), type, null, field.getKey());
      if (!field.getValue().accept(this, fieldValue)) return false;
    }
    
    return true;
  }

  @Override
  public Boolean visit(TuplePattern pattern, Obj type) {
    // Check each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = mChecker.getInterpreter().getQualifiedMember(
          Position.none(), type, null, Name.getTupleField(i));
      if (!fieldPattern.accept(this, field)) return false;
    }
    
    return true;
  }
  
  @Override
  public Boolean visit(ValuePattern pattern, Obj type) {
    // Make sure the type of the pattern's value is compatible with the type
    // being matched.
    Obj matchedType = mChecker.evaluateExpressionType(pattern.getValue());
    
    return checkTypes(type, matchedType);
  }

  @Override
  public Boolean visit(VariablePattern pattern, Obj type) {
    // If we don't have a type, allow anything.
    if (pattern.getType() == null) return true;

    // Make sure the type of the pattern's value is compatible with the type
    // being matched.
    Obj matchedType = mChecker.getInterpreter().evaluate(pattern.getType());
    return checkTypes(matchedType, type);
  }

  private PatternCheckingTester(Checker checker) {
    mChecker = checker;
  }
  
  private boolean checkTypes(Obj expected, Obj actual) {
    Obj matches = mChecker.getInterpreter().invokeMethod(expected,
        Name.ASSIGNS_FROM, actual);
    return matches.asBool();
  }
  
  private final Checker mChecker;
}
