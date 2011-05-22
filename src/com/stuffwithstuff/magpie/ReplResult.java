package com.stuffwithstuff.magpie;

public class ReplResult {
  ReplResult(String text, boolean isError) {
    mText = text;
    mIsError = isError;
  }
  
  public String getText() { return mText; }
  public boolean isError() { return mIsError; }
  
  private final String mText;
  private final boolean mIsError;
  
}
