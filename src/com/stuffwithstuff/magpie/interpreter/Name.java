package com.stuffwithstuff.magpie.interpreter;

/**
 * This defines identifiers that are defined or used within Magpie but also
 * referenced directly by the Java code.
 */
public final class Name {
  public static final String CALL = "call";
  public static final String COUNT = "count";
  public static final String CURRENT = "current";
  public static final String DECLARE_FIELD = "declareField";
  public static final String EQEQ = "==";
  public static final String IS_TRUE = "true?";
  public static final String IT = "it";
  public static final String ITERATE = "iterate";
  public static final String NEW = "new";
  public static final String NEXT = "next";
  public static final String NOTHING = "nothing";
  public static final String RECEIVING = "receiving";
  public static final String STRING = "string";

  public static boolean isPrivate(String name) {
    return name.startsWith("_");
  }
  
  public static String makeAssigner(String name) {
    return name + "_=";
  }
  
  public static String makeClassPrivate(String className, String member) {
    return className + ".private." + member;
  }
  
  public static String getTupleField(int index) {
    return "field" + index;
  }
  
  private Name() {
  }
}
