package com.stuffwithstuff.magpie.type;

public class NamedType implements TypeDecl {
  public NamedType(String name) {
    mName = name;
  }
  
  public String getName() { return mName; }
  
  private final String mName;
}
