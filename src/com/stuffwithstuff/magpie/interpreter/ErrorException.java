package com.stuffwithstuff.magpie.interpreter;

/**
 * This "exception" is used internally by the evaluator to implement
 * Magpie-level exceptions.
 */
@SuppressWarnings("serial")
public class ErrorException extends RuntimeException {
  public ErrorException(Obj error) {
    mError = error;
  }
  
  public Obj getError() { return mError; }
  
  private final Obj mError;
}