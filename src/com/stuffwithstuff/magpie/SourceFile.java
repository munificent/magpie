package com.stuffwithstuff.magpie;

public class SourceFile {
  public SourceFile(String path, String source) {
    mPath = path;
    mSource = source;
  }
  
  public String getPath() { return mPath; }
  public String getSource() { return mSource; }
  
  @Override
  public String toString() {
    return mPath;
  }
  
  private final String mPath;
  private final String mSource;
}
