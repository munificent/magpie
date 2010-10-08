package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.stuffwithstuff.magpie.parser.CharacterReader;

/**
 * Provides a string of characters by reading them from the user a line at a
 * time, as requested.
 */
public class ReplCharacterReader implements CharacterReader {
  public ReplCharacterReader() {
    InputStreamReader converter = new InputStreamReader(System.in);
    mInput = new BufferedReader(converter);
  }
  
  @Override
  public char current() {
    while (mPosition >= mLine.length()) {
      readLine();
    }

    return mLine.charAt(mPosition);
  }
  
  @Override
  public void advance() {
    if (mPosition < mLine.length()) {
      mPosition++;
    } else {
      readLine();
    }
  }

  @Override
  public String lookAhead(int count) {
    if (mPosition >= mLine.length()) return "";
    
    int endIndex = Math.min(mPosition + count, mLine.length());
    return mLine.substring(mPosition, endIndex);
  }

  private void readLine() {
    if (mFirstLine) {
      System.out.print(">> ");
      mFirstLine = false;
    } else {
      System.out.print(" | ");
    }
    
    try {
      mLine = mInput.readLine() + "\n";
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    mPosition = 0;
  }
  
  private final BufferedReader mInput;
  private boolean mFirstLine = true;
  private String mLine = "";
  private int mPosition = 0;
}
