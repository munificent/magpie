package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;


/**
 * Represents a field in a class object. Unlike Field in the ast package, this
 * is used to represent a "live" field in a ClassObj. In particular, the field
 * initializers are full closures instead of just raw expressions.
 */
public class FieldObj {
  public FieldObj(Callable initializer, Pattern pattern) {
    mInitializer = initializer;
    mPattern = pattern;
  }

  /**
   * Gets the initializer for this field. Returns null if the field is just
   * declared.
   */
  public Callable getInitializer() { return mInitializer; }
  
  /**
   * Gets the pattern for this field.
   */
  public Pattern getPattern() { return mPattern; }
  
  private final Callable mInitializer;
  private final Pattern  mPattern;
}
