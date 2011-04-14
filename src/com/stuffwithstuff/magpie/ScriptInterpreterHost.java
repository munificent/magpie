package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.InterpreterHost;
import com.stuffwithstuff.magpie.interpreter.ModuleSource;

public class ScriptInterpreterHost implements InterpreterHost {
  @Override
  public boolean allowTopLevelRedefinition() {
    return false;
  }

  @Override
  public void print(String text) {
    System.out.print(text);
  }

  @Override
  public ModuleSource loadModule(String scriptPath, String name) {
    return Script.loadModule(scriptPath, name);
  }
}
