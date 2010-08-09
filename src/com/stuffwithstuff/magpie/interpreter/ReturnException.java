package com.stuffwithstuff.magpie.interpreter;

@SuppressWarnings("serial")
public class ReturnException extends RuntimeException {
  public ReturnException(Obj value) {
    mValue = value;
  }
  
  public Obj getValue() { return mValue; }
  
  private final Obj mValue;
}