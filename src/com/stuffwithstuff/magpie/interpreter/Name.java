package com.stuffwithstuff.magpie.interpreter;

/**
 * This defines identifiers that are defined or used within Magpie but also
 * referenced directly by the Java code.
 */
public final class Name {
  public static final String AMBIGUOUS_METHOD_ERROR = "AmbiguousMethodError";
  public static final String CALL = "call";
  public static final String CLASS = "Class";
  public static final String COUNT = "count";
  public static final String CURRENT = "current";
  public static final String DECLARE_FIELD = "declareField";
  public static final String EQEQ = "==";
  public static final String INIT = "init";
  public static final String IO_ERROR = "IOError";
  public static final String IS_TRUE = "isTrue";
  public static final String IT = "it";
  public static final String ITERATE = "iterate";
  public static final String INITIALIZATION_ERROR = "InitializationError";
  public static final String NEW = "new";
  public static final String NEXT = "next";
  public static final String NO_MATCH_ERROR = "NoMatchError";
  public static final String NO_METHOD_ERROR = "NoMethodError";
  public static final String NO_VARIABLE_ERROR = "NoVariableError";
  public static final String NOTHING = "nothing";
  public static final String OUT_OF_BOUNDS_ERROR = "OutOfBoundsError";
  public static final String PARENT_COLLISION_ERROR = "ParentCollisionError";
  public static final String PARSE_ERROR = "ParseError";
  public static final String REDEFINITION_ERROR = "RedefinitionError";
  public static final String TO_STRING = "toString";

  public static boolean isPublic(String name) {
    return !name.startsWith("_");
  }
  
  public static String makeAssigner(String name) {
    return name + " =";
  }
  
  public static String makeClassPrivate(String className, String member) {
    return className + ".private." + member;
  }
  
  public static String getTupleField(int index) {
    return Integer.toString(index);
  }
  
  private Name() {
  }
}
