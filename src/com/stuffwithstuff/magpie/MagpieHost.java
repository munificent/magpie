package com.stuffwithstuff.magpie;

public interface MagpieHost {
  SourceFile loadModule(String name);
  void showSyntaxError(String message);
}
