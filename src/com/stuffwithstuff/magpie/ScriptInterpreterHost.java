package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class ScriptInterpreterHost implements InterpreterHost {
  @Override
  public boolean allowTopLevelRedefinition() {
    return false;
  }

  @Override
  public void print(String text) {
    System.out.print(text);
  }
}
