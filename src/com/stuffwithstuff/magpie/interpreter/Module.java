package com.stuffwithstuff.magpie.interpreter;

public class Module {
  public Module(ModuleInfo info, Scope parent) {
    mInfo = info;
    mScope = new Scope(parent);
  }
  
  public String getName() { return mInfo.getName(); }
  public String getPath() { return mInfo.getPath(); }
  public String getSource() { return mInfo.getSource(); }
  public Scope getScope() { return mScope; }
  
  public void importTo(Scope scope) {
    mScope.importTo(scope);
  }
  
  private final ModuleInfo mInfo;
  private final Scope mScope;
}
