package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.parser.Position;

public class NullInterpreterHost implements InterpreterHost {
  public void print(String text) {}
  
  public void runtimeError(Position position, String message) {}
}
