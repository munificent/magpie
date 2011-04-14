package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.parser.CharacterReader;

public class ModuleSource {
  public ModuleSource(String path, CharacterReader reader) {
    mPath = path;
    mReader = reader;
  }
  
  public String getPath() { return mPath; }
  public CharacterReader getReader() { return mReader; }
  
  private final String mPath;
  private final CharacterReader mReader;
}
