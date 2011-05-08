package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.SourceReader;


/**
 * Reads a string, one character at a time.
 */
public class StringReader implements SourceReader {
  public StringReader(String description, String text) {
    mDescription = description;
    mText = text;
    mPosition = 0;
  }
  
  @Override
  public String getDescription() { return mDescription; }
  
  @Override
  public char current() {
    if (mPosition >= mText.length()) return '\0';
    return mText.charAt(mPosition);
  }
  
  @Override
  public void advance() {
    if (mPosition < mText.length()) mPosition++;
  }

  private final String mDescription;
  private final String mText;
  private int mPosition;
}
