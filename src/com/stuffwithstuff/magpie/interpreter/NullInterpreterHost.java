package com.stuffwithstuff.magpie.interpreter;

public class NullInterpreterHost implements InterpreterHost {
  @Override
  public boolean allowTopLevelRedefinition() {
    // Do nothing.
    return false;
  }
  
  @Override
  public void print(String text) {
    // Do nothing.
  }

  @Override
  public ModuleInfo loadModule(String name) {
    throw new UnsupportedOperationException();
  }
}
