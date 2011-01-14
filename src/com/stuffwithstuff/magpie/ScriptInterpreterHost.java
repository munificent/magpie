package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.InterpreterHost;
import com.stuffwithstuff.magpie.parser.Position;

public class ScriptInterpreterHost implements InterpreterHost {

  @Override
  public void print(String text) {
    System.out.print(text);
  }

  @Override
  public void runtimeError(Position position, String message) {
    System.out.println(String.format("Error at %s: %s",
        position, message));
  }
}
