package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.PatternVisitor;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.ast.pattern.TypePattern;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;
import com.stuffwithstuff.magpie.ast.pattern.WildcardPattern;

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
 */
public class PatternComparer {
  public static enum Result {
    LESS,
    GREATER,
    SAME,
    NONE;
    
    Result reverse() {
      switch (this) {
      case LESS: return GREATER;
      case GREATER: return LESS;
      default: return this;
      }
    }
  }
  
  public PatternComparer(Context context) {
    mContext = context;
  }
  
  public Result compare(Callable method1, Callable method2) {
    return compare(method1.getPattern(), method1.getClosure(),
                   method2.getPattern(), method2.getClosure());
  }

  private Result compare(Pattern pattern1, Scope scope1,
      Pattern pattern2, Scope scope2) {
    pattern1 = pattern1.accept(new PatternSimplifier(), null);
    pattern2 = pattern2.accept(new PatternSimplifier(), null);
    
    PatternKind kind1 = pattern1.accept(new PatternKinder(), null);
    PatternKind kind2 = pattern2.accept(new PatternKinder(), null);
    
    // Ironically, this bit of code would really benefit from multiple dispatch.
    switch (kind1) {
    case ANY:
      switch (kind2) {
      case ANY:     return Result.SAME;
      case RECORD:  return Result.LESS;
      case TYPE:    return Result.LESS;
      case VALUE:   return Result.LESS;
      default:
        throw new UnsupportedOperationException("Unknown pattern kind.");
      }
    case RECORD:
      switch (kind2) {
      case ANY:     return Result.GREATER;
      case RECORD:  return compareRecords(pattern1, scope1, pattern2, scope2);
      case TYPE:    return Result.GREATER;
      case VALUE:   return Result.LESS;
      default:
        throw new UnsupportedOperationException("Unknown pattern kind.");
      }
    case TYPE:
      switch (kind2) {
      case ANY:     return Result.GREATER;
      case RECORD:  return Result.LESS;
      case TYPE:    return compareTypes(pattern1, scope1, pattern2, scope2);
      case VALUE:   return Result.LESS;
      default:
        throw new UnsupportedOperationException("Unknown pattern kind.");
      }
    case VALUE:
      switch (kind2) {
      case ANY:     return Result.GREATER;
      case RECORD:  return Result.GREATER;
      case TYPE:    return Result.GREATER;
      case VALUE:   return compareValues(pattern1, scope1, pattern2, scope2);
      default:
        throw new UnsupportedOperationException("Unknown pattern kind.");
      }
    default:
      throw new UnsupportedOperationException("Unknown pattern kind.");
    }
  }

  private Set<String> intersect(Set<String> a, Set<String> b) {
    Set<String> intersect = new HashSet<String>();
    for (String field : a) {
      if (b.contains(field)) intersect.add(field);
    }
    
    return intersect;
  }

  /**
   * Returns true if a contains elements that are not in b.
   */
  private boolean containsOthers(Set<String> a, Set<String> b) {
    for (String field : a) {
      if (!b.contains(field)) return true;
    }
    
    return false;
  }
  
  private Result compareRecords(Pattern pattern1, Scope scope1,
      Pattern pattern2, Scope scope2) {
    Map<String, Pattern> record1 = ((RecordPattern)pattern1).getFields();
    Map<String, Pattern> record2 = ((RecordPattern)pattern2).getFields();
    
    // Take the intersection of their fields.
    Set<String> intersect = intersect(record1.keySet(), record2.keySet());
    
    // Which record are we leaning towards preferring?
    Result lean = Result.SAME;
    
    // If the records don't have the same number of fields, one must be a
    // strict superset of the other.
    if ((record1.size() != intersect.size()) ||
        (record2.size() != intersect.size())) {
      if (containsOthers(record1.keySet(), record2.keySet()) &&
          containsOthers(record2.keySet(), record1.keySet())) {
        return Result.NONE;
      } else {
        // Lean towards the superset.
        lean = (record1.size() > record2.size()) ? Result.GREATER : Result.LESS;
      }
    }

    // Fields that are common to the two cannot disagree on sort order.
    for (String name : intersect) {
      Pattern field1 = record1.get(name);
      Pattern field2 = record2.get(name);
      
      Result compare = compare(field1, scope1, field2, scope2);
      if (compare == Result.NONE) return Result.NONE;
      
      if (lean == Result.SAME) {
        lean = compare;
      } else if (compare == Result.SAME) {
        // Do nothing.
      } else if (compare != lean) {
        // If we get here, the fields don't agree.
        return Result.NONE;
      }
    }
    
    return lean;
  }
  
  private Result compareTypes(Pattern pattern1, Scope scope1,
      Pattern pattern2, Scope scope2) {
    Obj type1 = mContext.evaluate(((TypePattern)pattern1).getType(), scope1);
    Obj type2 = mContext.evaluate(((TypePattern)pattern2).getType(), scope2);
    
    // TODO(bob): WIP getting rid of types.
    if (type1 instanceof ClassObj && type2 instanceof ClassObj) {
      ClassObj class1 = (ClassObj)type1;
      ClassObj class2 = (ClassObj)type2;
      
      // Same class.
      if (class1 == class2) return Result.SAME;
      
      if (class1.isSubclassOf(class2)) {
        // Class1 is a subclass, so it's more specific.
        return Result.GREATER;
      } else if (class2.isSubclassOf(class1)) {
        // Class2 is a subclass, so it's more specific.
        return Result.LESS;
      } else {
        // No class relation between the two, so they can't be ordered.
        return Result.NONE;
      }
    }
    
    throw new UnsupportedOperationException("Must be class now!");
  }

  private Result compareValues(Pattern pattern1, Scope scope1,
      Pattern pattern2, Scope scope2) {
    Obj value1 = mContext.evaluate(((ValuePattern)pattern1).getValue(), scope1);
    Obj value2 = mContext.evaluate(((ValuePattern)pattern2).getValue(), scope2);
    
    // Identical values are ordered the same. This lets us have tuples with
    // some identical value fields (like nothing) which are then sorted by
    // other fields.
    if (mContext.objectsEqual(value1, value2)) return Result.SAME;
    
    // Any other paid of values can't be sorted.
    return Result.NONE;
  }

  private enum PatternKind {
    ANY,
    RECORD,
    TYPE,
    VALUE
  }
  
  /**
   * Removes all variable patterns from a pattern since the linearizer doesn't
   * care about them.
   */
  private static class PatternSimplifier implements PatternVisitor<Pattern, Void> {
    @Override
    public Pattern visit(RecordPattern pattern, Void dummy) {
      Map<String, Pattern> fields = new HashMap<String, Pattern>();
      for (Entry<String, Pattern> field : pattern.getFields().entrySet()) {
        fields.put(field.getKey(), field.getValue().accept(this, null));
      }
      
      return Pattern.record(fields);
    }
    
    @Override
    public Pattern visit(TypePattern pattern, Void dummy) {
      return pattern;
    }

    @Override
    public Pattern visit(ValuePattern pattern, Void dummy) {
      return pattern;
    }

    @Override
    public Pattern visit(VariablePattern pattern, Void dummy) {
      return pattern.getPattern().accept(this, null);
    }

    @Override
    public Pattern visit(WildcardPattern pattern, Void dummy) {
      return pattern;
    }
  }
  
  private static class PatternKinder implements PatternVisitor<PatternKind, Void> {
    @Override
    public PatternKind visit(RecordPattern pattern, Void dummy) {
      return PatternKind.RECORD;
    }
    
    @Override
    public PatternKind visit(TypePattern pattern, Void dummy) {
      return PatternKind.TYPE;
    }

    @Override
    public PatternKind visit(ValuePattern pattern, Void dummy) {
      return PatternKind.VALUE;
    }

    @Override
    public PatternKind visit(VariablePattern pattern, Void dummy) {
      return pattern.getPattern().accept(this, null);
    }

    @Override
    public PatternKind visit(WildcardPattern pattern, Void dummy) {
      return PatternKind.ANY;
    }
  }
  
  private final Context mContext;
}
