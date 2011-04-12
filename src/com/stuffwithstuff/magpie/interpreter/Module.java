package com.stuffwithstuff.magpie.interpreter;

public class Module {
  public Module(String name, boolean allowTopLevelRedefinition) {
    mName = name;
    mScope = new Scope(allowTopLevelRedefinition);
  }
  
  public String getName() { return mName; }
  public Scope getScope() { return mScope; }
  
  private final String mName;
  private final Scope mScope;
}
