package com.stuffwithstuff.magpie.interpreter;

/**
 * Represents a declared or defined field in a class. This describes the field
 * itself from the class's perspective. It is not a field *value* in a
 * particular instance of a class. (Those just use a regular Scope.)
 */
public class Field {
  public Field(boolean hasInitializer, FnObj definition) {
    mHasInitializer = hasInitializer;
    mDefinition = definition;
  }
  
  /**
   * Gets whether this field is declared (only its type is specified) or defined
   * (it has an initializer expression).
   * 
   * @return true if the field has an initializer.
   */
  public boolean hasInitializer() { return mHasInitializer; }
  
  /**
   * Gets the definition for this field. If it has an initializer, this will
   * return the initializing expression. If not, this will return the type
   * annotation expression.
   * 
   * @return The definition for the field.
   */
  public FnObj getDefinition() { return mDefinition; }
  
  private final boolean mHasInitializer;
  private final FnObj   mDefinition;
}
