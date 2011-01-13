package com.stuffwithstuff.magpie.util;

@SuppressWarnings("serial")
public class NotImplementedException extends RuntimeException {
  public NotImplementedException(String message) {
    super(message);
  }
  
  public NotImplementedException() {
    super("This feature isn't implemented yet. :(");
  }
}
