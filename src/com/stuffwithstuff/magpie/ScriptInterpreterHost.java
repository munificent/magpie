package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class ScriptInterpreterHost implements InterpreterHost {

  @Override
  public void print(String text) {
    System.out.println(text);
  }

  @Override
  public void runtimeError(Position position, String message) {
    System.out.println(String.format("Error at %s: %s",
        position, message));
  }
}
