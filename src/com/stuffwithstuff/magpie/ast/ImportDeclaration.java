package com.stuffwithstuff.magpie.ast;

public class ImportDeclaration {
  public ImportDeclaration(boolean isExported, String name, String rename) {
    mIsExported = isExported;
    mName = name;
    mRename = rename;
  }
  
  public boolean isExported() { return mIsExported; }
  public String getName() { return mName; }
  public String getRename() { return mRename; }

  private final boolean mIsExported;
  private final String mName;
  private final String mRename;
}
