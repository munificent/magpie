package com.stuffwithstuff.magpie;



public interface MagpieHost {
  void print(String text);
  SourceFile loadModule(String name);
}
