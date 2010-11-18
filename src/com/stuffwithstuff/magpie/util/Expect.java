package com.stuffwithstuff.magpie.util;

public final class Expect {
  public static void notEmpty(String arg) {
    if (arg == null) {
      throw new NullPointerException("String cannot be null.");
    }
    if (arg.length() == 0) {
      throw new IllegalArgumentException("String cannot be empty.");
    }
  }
  
  public static void notNull(Object arg) {
    if (arg == null) {
      throw new NullPointerException("Argument cannot be null.");
    }
  }
  
  public static void positive(int arg) {
    if (arg <= 0) {
      throw new IllegalArgumentException("Argument must be positive.");
    }
  }
  
  private Expect() {}
}
