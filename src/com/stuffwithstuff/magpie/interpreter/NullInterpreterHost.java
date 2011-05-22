package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.MagpieHost;
import com.stuffwithstuff.magpie.SourceFile;


public class NullInterpreterHost implements MagpieHost {
  @Override
  public SourceFile loadModule(String name) {
    throw new UnsupportedOperationException();
  }
}
