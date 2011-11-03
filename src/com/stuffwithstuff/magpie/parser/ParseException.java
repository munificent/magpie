package com.stuffwithstuff.magpie.parser;

@SuppressWarnings("serial")
public class ParseException extends RuntimeException {
  public ParseException(Position position, String message) {
    super(message);
    mPosition = position;
  }
  
  public Position getPosition() { return mPosition; }
  
  private final Position mPosition;
}
