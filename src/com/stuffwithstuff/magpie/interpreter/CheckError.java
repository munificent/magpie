package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * An error that occurred during type-checking.
 */
public class CheckError {
  public CheckError(Position position, String message) {
    mPosition = position;
    mMessage = message;
  }
  
  public Position getPosition() { return mPosition; }
  public int      getLine()     { return mPosition.getStartLine(); }
  public String   getMessage()  { return mMessage; }
  
  public String toString() {
    return mPosition + ": " + mMessage;
  }
  
  private final Position mPosition;
  private final String mMessage;
}
