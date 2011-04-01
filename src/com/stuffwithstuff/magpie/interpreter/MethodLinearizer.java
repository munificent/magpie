package com.stuffwithstuff.magpie.interpreter;

import java.util.Comparator;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.ast.pattern.TuplePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.util.NotImplementedException;

/**
 * Compares two methods to see which takes precedence over the other. It is
 * important that both methods be applicable to some single argument type or
 * ambiguous method errors may result. For example, two unrelated classes like
 * Int and Bool can't be linearized since there is no relationship between them.
 * However, by restricting this to applicable methods, that case should be
 * avoided since there's no single argument value for which both of those is
 * applicable.
 * 
 * The basic linearization process is relatively simple. First we determine the
 * kind of the two methods. There are four kinds that matter:
 * - Value patterns
 * - Nominal (i.e. class) type patterns
 * - Structural (i.e. record or tuple) type patterns
 * - Any (i.e. wildcard or simple variable name) patterns
 * 
 * Earlier kinds take precedence of later kinds in the above list, so a method
 * specialized to a value will always win over one specialized to a class.
 * 
 * If comparing kinds doesn't yield an ordering, we compare within kind. For
 * value or any patterns, it is an error to have multiple methods with that
 * kind and an error is raised. For type patterns, we then compare the types.
 * 
 * Classes are linearized based on their inheritance tree. Subclasses take
 * precedence over superclasses. Later siblings take precedence over earlier
 * ones. (If D inherits from A and B, in that order, B takes precedence over A.)
 * Since class hierarchies are strict trees, this is enough to order two
 * classes.
 * 
 * With structural types, their fields are linearized. If all fields sort the
 * same way (or are the same) then the type with the winning fields wins. For
 * example, (Derived, Int) beats (Base, Int).
 * 
 * TODO(bob): Note that only a fraction of the above is currently implemented.
 * It's getting there...
 * TODO(bob): What about OrTypes, function types, generics, and user-defined
 * types?
 */
public class MethodLinearizer implements Comparator<Callable> {
  public MethodLinearizer(Interpreter interpreter) {
    mInterpreter = interpreter;
  }

  @Override
  public int compare(Callable method1, Callable method2) {
    return compare(method1.getPattern(),
                   method2.getPattern());
  }

  private int compare(Pattern pattern1, Pattern pattern2) {
    final int firstWins = -1;
    final int secondWins = 1;
    
    // Ironically, this bit of code would really benefit from multiple dispatch.
    if (isAny(pattern1)) {
      if      (isAny(pattern2))    return 0;
      else if (isRecord(pattern2)) return secondWins;
      else if (isTuple(pattern2))  return secondWins;
      else if (isType(pattern2))   return secondWins;
      else if (isValue(pattern2))  return secondWins;
      else throw new UnsupportedOperationException("Unknown pattern type.");
    } else if (isRecord(pattern1)) {
      if      (isAny(pattern2))    return firstWins;
      else if (isRecord(pattern2)) throw new NotImplementedException();
      else if (isTuple(pattern2))  throw new NotImplementedException();
      else if (isType(pattern2))   return compareTypes(pattern1, pattern2);
      else if (isValue(pattern2))  return secondWins;
      else throw new UnsupportedOperationException("Unknown pattern type.");
    } else if (isTuple(pattern1)) {
      if      (isAny(pattern2))    return firstWins;
      else if (isRecord(pattern2)) throw new NotImplementedException();
      else if (isTuple(pattern2))  return compareTuples(pattern1, pattern2);
      else if (isType(pattern2))   return compareTypes(pattern1, pattern2);
      else if (isValue(pattern2))  return secondWins;
      else throw new UnsupportedOperationException("Unknown pattern type.");
    } else if (isType(pattern1)) {
      if      (isAny(pattern2))    return firstWins;
      else if (isRecord(pattern2)) return compareTypes(pattern1, pattern2);
      else if (isTuple(pattern2))  return compareTypes(pattern1, pattern2);
      else if (isType(pattern2))   return compareTypes(pattern1, pattern2);
      else if (isValue(pattern2))  return secondWins;
      else throw new UnsupportedOperationException("Unknown pattern type.");
    } else if (isValue(pattern1)) {
      if      (isAny(pattern2))    return firstWins;
      else if (isRecord(pattern2)) return firstWins;
      else if (isTuple(pattern2))  return firstWins;
      else if (isType(pattern2))   return firstWins;
      else if (isValue(pattern2))  return compareValues(pattern1, pattern2);
      else throw new UnsupportedOperationException("Unknown pattern type.");
    } else {
      throw new UnsupportedOperationException("Unknown pattern type.");
    }
  }

  private int compareTuples(Pattern pattern1, Pattern pattern2) {
    TuplePattern tuple1 = (TuplePattern)pattern1;
    TuplePattern tuple2 = (TuplePattern)pattern2;
    
    // TODO(bob): Eventually should handle different-sized tuples.
    if (tuple1.getFields().size() != tuple2.getFields().size()) {
      throw ambiguous(pattern1, pattern2);
    }
    
    int currentComparison = 0;
    for (int i = 0; i < tuple1.getFields().size(); i++) {
      int compare = compare(tuple1.getFields().get(i),
                            tuple2.getFields().get(i));
      
      if (currentComparison == 0) {
        currentComparison = compare;
      } else if (compare == 0) {
        // Do nothing.
      } else if (compare != currentComparison) {
        // If we get here, the fields don't agree.
        throw ambiguous(pattern1, pattern2);
      }
    }
    
    return currentComparison;
  }
  
  private int compareTypes(Pattern pattern1, Pattern pattern2) {
    Obj type1 = mInterpreter.evaluate(PatternTyper.evaluate(pattern1));
    Obj type2 = mInterpreter.evaluate(PatternTyper.evaluate(pattern2));
    
    // TODO(bob): WIP getting rid of types.
    if (type1 instanceof ClassObj && type2 instanceof ClassObj) {
      ClassObj class1 = (ClassObj)type1;
      ClassObj class2 = (ClassObj)type2;
      
      // Same class.
      if (class1 == class2) return 0;
      
      if (class1.isSubclassOf(class2)) {
        // Class1 is a subclass, so it's more specific.
        return -1;
      } else if (class2.isSubclassOf(class1)) {
        // Class2 is a subclass, so it's more specific.
        return 1;
      } else {
        // No class relation between the two, so they can't be linearized.
        throw ambiguous(pattern1, pattern2);
      }
    }
    
    throw new UnsupportedOperationException("Must be class now!");
  }

  private int compareValues(Pattern pattern1, Pattern pattern2) {
    Obj value1 = mInterpreter.evaluate(PatternTyper.evaluate(pattern1));
    Obj value2 = mInterpreter.evaluate(PatternTyper.evaluate(pattern2));
    
    // Identical values are ordered the same. This lets us have tuples with
    // some identical value fields (like nothing) which are then sorted by
    // other fields.
    if (mInterpreter.objectsEqual(value1, value2)) return 0;
    
    // Any other paid of values can't be sorted.
    throw ambiguous(pattern1, pattern2);
  }
  
  private ErrorException ambiguous(Pattern pattern1, Pattern pattern2) {
    return mInterpreter.error("AmbiguousMethodError", 
        "Cannot choose a method between " + pattern1 + " and " + pattern2);
  }
  
  private boolean isAny(Pattern pattern) {
    return (pattern instanceof VariablePattern) &&
        (((VariablePattern)pattern).getType() == null);
  }
  
  private boolean isRecord(Pattern pattern) {
    return pattern instanceof RecordPattern;
  }
  
  private boolean isTuple(Pattern pattern) {
    return pattern instanceof TuplePattern;
  }
  
  private boolean isType(Pattern pattern) {
    return (pattern instanceof VariablePattern) &&
        (((VariablePattern)pattern).getType() != null);
  }
  
  private boolean isValue(Pattern pattern) {
    return pattern instanceof ValuePattern;
  }

  private final Interpreter mInterpreter;
}
