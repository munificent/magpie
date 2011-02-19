package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class ScriptInterpreterHost implements InterpreterHost {

  @Override
  public void print(String text) {
    System.out.print(text);
  }
}
