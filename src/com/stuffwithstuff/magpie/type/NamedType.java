package com.stuffwithstuff.magpie.type;

public class NamedType extends TypeDecl {
  public NamedType(String name) {
    mName = name;
  }
  
  public String getName() { return mName; }
  
  private final String mName;
}
