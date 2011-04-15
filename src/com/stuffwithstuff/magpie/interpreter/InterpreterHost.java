package com.stuffwithstuff.magpie.interpreter;

public interface InterpreterHost {
  boolean allowTopLevelRedefinition();
  void print(String text);
  ModuleInfo loadModule(Module loadingModule, String name);
}
