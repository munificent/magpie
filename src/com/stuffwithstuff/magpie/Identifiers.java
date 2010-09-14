package com.stuffwithstuff.magpie;

/**
 * This defines identifiers that are defined or used within Magpie but also
 * referenced directly by the Java code.
 */
public final class Identifiers {
  public static final String CALL = "call";
  public static final String CALL_ASSIGN = "call=";
  public static final String CAN_ASSIGN_FROM = "canAssignFrom";
  public static final String COUNT = "count";
  public static final String CURRENT = "current";
  public static final String DECLARE_FIELD = "declareField";
  public static final String DECLARE_METHOD = "declareMethod";
  public static final String DEFINE_CONSTRUCTOR = "defineConstructor";
  public static final String DEFINE_FIELD = "defineField";
  public static final String DEFINE_METHOD = "defineMethod";
  public static final String EQUALS = "==";
  public static final String GET_METHOD_TYPE = "getMethodType";
  public static final String IS_TRUE = "true?";
  public static final String ITERATE = "iterate";
  public static final String NEW = "new";
  public static final String NEW_TYPE = "newType";
  public static final String NEXT = "next";
  public static final String OR = "|";
  public static final String TO_STRING = "toString";
  public static final String TYPE = "type";
  public static final String UNSAFE_REMOVE_NOTHING = "unsafeRemoveNothing";
  
  /**
   * Gets whether or not this method name could be a setter (i.e. "foo=").
   * @param   name  The full name of the method.
   * @return        True if the name is a setter.
   */
  public static boolean isSetter(String name) {
    return name.endsWith("=");
  }
  
  /**
   * Given a setter name like "foo=", returns the base name "foo".
   * @param  setter  The name of the setter method.
   * @return         The base name of the field being set.
   */
  public static String getSetterBaseName(String setter) {
    return setter.substring(0, setter.length() - 1);
  }
  
  public static String makeSetter(String baseName) {
    return baseName + "=";
  }
  
  private Identifiers() {}
}
