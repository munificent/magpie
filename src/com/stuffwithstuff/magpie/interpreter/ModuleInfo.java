package com.stuffwithstuff.magpie.interpreter;

public class ModuleInfo {
  public ModuleInfo(String name, String path, String source) {
    mName = name;
    mPath = path;
    mSource = source;
  }
  
  public String getName() { return mName; }
  public String getPath() { return mPath; }
  public String getSource() { return mSource; }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private final String mName;
  private final String mPath;
  private final String mSource;
}
