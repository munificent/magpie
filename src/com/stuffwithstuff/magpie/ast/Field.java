package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;


/**
 * Represents a declared or defined field in a class. This describes the field
 * itself from the class's perspective. It is not a field *value* in a
 * particular instance of a class. (Those just use a regular Scope.)
 */
public class Field {
  public Field(boolean isMutable, Expr initializer, Expr type) {
    mIsMutable = isMutable;
    mInitializer = initializer;
    mType = type;
  }

  /**
   * Gets whether or not the field is mutable.
   */
  public boolean isMutable() { return mIsMutable; }
  
  /**
   * Gets the initializer for this field. Returns null if the field is just
   * declared.
   */
  public Expr getInitializer() { return mInitializer; }
  
  /**
   * Gets the type annotation for this field. Will be null if the field has an
   * initializer.
   */
  public Expr getType() { return mType; }
  
  public Pattern getPattern() {
    if (mType != null) {
      return Pattern.type(mType);
    } else {
      return Pattern.wildcard();
    }
  }
  
  private final boolean  mIsMutable;
  private final Expr     mInitializer;
  private final Expr     mType;
}
