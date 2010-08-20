package com.stuffwithstuff.magpie.util;

public final class Expect {
  public static void notEmpty(String arg) {
    if (arg == null) throw new NullPointerException("String cannot be null.");
    if (arg.length() == 0) throw new IllegalArgumentException("String cannot be empty.");
  }
  
  public static void notNull(Object arg) {
    if (arg == null) throw new NullPointerException("Argument cannot be null.");
  }
  
  private Expect() {}
}
