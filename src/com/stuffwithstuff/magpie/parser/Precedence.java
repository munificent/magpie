package com.stuffwithstuff.magpie.parser;

public class Precedence {
  public static final int MESSAGE     = 10;
  public static final int UNARY       =  9;  // not -
  public static final int PRODUCT     =  8;  // * / %
  public static final int TERM        =  7;  // + -
  public static final int COMPARISON  =  5;  // < > <= >=
  public static final int EQUALITY    =  4;  // == !=
  public static final int LOGICAL     =  3;  // and or
  public static final int COMPOSITION =  2;  // , record
  public static final int ASSIGNMENT  =  1;
}
