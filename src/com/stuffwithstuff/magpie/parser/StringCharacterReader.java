package com.stuffwithstuff.magpie.parser;


/**
 * Reads a string, one character at a time.
 */
public class StringCharacterReader implements CharacterReader {
  public StringCharacterReader(String text) {
    mText = text;
    mPosition = 0;
  }
  
  @Override
  public char current() {
    if (mPosition >= mText.length()) return '\0';
    return mText.charAt(mPosition);
  }
  
  @Override
  public void advance() {
    if (mPosition < mText.length()) mPosition++;
  }

  @Override
  public String lookAhead(int count) {
    if (mPosition >= mText.length()) return "";
    
    int endIndex = Math.min(mPosition + count, mText.length());
    return mText.substring(mPosition, endIndex);
  }

  private final String mText;
  private int mPosition;
}
