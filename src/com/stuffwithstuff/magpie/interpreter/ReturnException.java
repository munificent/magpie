package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.util.Expect;

@SuppressWarnings("serial")
public class ReturnException extends RuntimeException {
  public ReturnException(Obj value) {
    Expect.notNull(value);
    
    mValue = value;
  }
  
  public Obj getValue() { return mValue; }
  
  private final Obj mValue;
}