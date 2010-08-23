package com.stuffwithstuff.magpie;

public final class Identifiers {
  public static final String TYPE = "type";
  public static final String EQUALS = "==";
  public static final String NEW_TYPE = "newType";
  public static final String CALL = "call";
  public static final String OR = "|";
  public static final String GET_METHOD_TYPE = "getMethodType";
  public static final String CAN_ASSIGN_FROM = "canAssignFrom";
  public static final String TO_STRING = "toString";
  public static final String NEW = "new";
  public static final String IS_TRUE = "true?";
  public static final String ITERATE = "iterate";
  public static final String NEXT = "next";
  public static final String CURRENT = "current";
  
  public static String makeSetter(String baseName) {
    return baseName + "=";
  }
  
  private Identifiers() {}
}
