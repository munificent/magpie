package com.stuffwithstuff.magpie.interpreter;

public class Module {
  public Module(String name, String path, Scope global) {
    mName = name;
    mPath = path;
    mScope = new Scope(global);
  }
  
  public String getName() { return mName; }
  public String getPath() { return mPath; }
  public Scope getScope() { return mScope; }
  
  public void importTo(Scope scope) {
    mScope.importTo(scope);
  }
  
  private final String mName;
  private final String mPath;
  private final Scope mScope;
}
