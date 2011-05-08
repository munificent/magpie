package com.stuffwithstuff.magpie.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.stuffwithstuff.magpie.SourceReader;

/**
 * Provides a string of characters by reading them from the user a line at a
 * time, as requested.
 */
public class ReplReader implements SourceReader {
  public ReplReader() {
    InputStreamReader converter = new InputStreamReader(System.in);
    mInput = new BufferedReader(converter);
  }
  
  @Override
  public String getDescription() {
    return "<repl>";
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

  protected void showPrompt(String prompt) {
    System.out.print(prompt);
  }
  
  protected void afterReadLine(String prompt, String line) {
    // Do nothing.
  }
  
  private void readLine() {
    String prompt;
    if (mFirstLine) {
      prompt = "> ";
      mFirstLine = false;
    } else {
      prompt = "| ";
    }
    
    showPrompt(prompt);
    
    try {
      mLine = mInput.readLine();
      
      afterReadLine(prompt, mLine);
      
      mLine = mLine + "\n";
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
