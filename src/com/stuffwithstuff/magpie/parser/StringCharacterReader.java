package com.stuffwithstuff.magpie.parser;


/**
 * Reads a string, one character at a time.
 */
public class StringCharacterReader implements CharacterReader {
  public StringCharacterReader(String description, String text) {
    mDescription = description;
    mText = text;
    mPosition = 0;
  }
  
  public String getText() { return mText; }
  
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
