package com.stuffwithstuff.magpie;

public class Term {
  public enum ForeColor {
    BLACK("30"),
    GRAY("1;30"),
    LIGHT_GRAY("37"),
    WHITE("1;37"),
    RED("31"),
    PINK("1;31"),
    GREEN("32"),
    LIGHT_GREEN("1;32"),
    ORANGE("33"),
    YELLOW("1;33"),
    BLUE("34"),
    LIGHT_BLUE("1;34"),
    PURPLE("35"),
    LAVENDER("1;35"),
    CYAN("36"),
    LIGHT_CYAN("1;36");
    
    ForeColor(String escape) {
      mEscape = escape;
    }
    
    public String escape() { return mEscape; }
    
    private final String mEscape;
  }
  
  public static void restoreColor() {
    System.out.print("\033[0m");
  }
  
  public static void set(ForeColor color) {
    System.out.print("\033[" + color.escape() + "m");
  }
}
