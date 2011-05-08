package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.MagpieHost;
import com.stuffwithstuff.magpie.SourceFile;

public class MagpieAppHost implements MagpieHost {
  @Override
  public void print(String text) {
    System.out.print(text);
  }

  @Override
  public SourceFile loadModule(String name) {
    return MagpieApp.loadModule(name);
  }
}
