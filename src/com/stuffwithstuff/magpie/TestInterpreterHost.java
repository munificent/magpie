package com.stuffwithstuff.magpie;

import java.io.IOException;
import java.util.*;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class TestInterpreterHost implements InterpreterHost {
  public TestInterpreterHost(String path) {
    mPath = path;
    mSuccess = true;
  }
  
  public boolean run(String source) {
    try {
      // Parse the expected output.
      for (String line : source.split("\n")) {
        int index = line.indexOf("//:");
        if (index != -1) {
          String expect = line.substring(index + 3).trim();
          mExpectedOutput.add(expect);
        }
      }
      
      Lexer lexer = new Lexer(source);
      MagpieParser parser = new MagpieParser(lexer);

      Interpreter interpreter = new Interpreter(this);
      interpreter.run(parser.parse());

      if (mExpectedOutput.size() > 0) {
        fail("Ran out of output when still expecting \"" +
            mExpectedOutput.poll() + "\".");
      }
    } catch (ParseException ex) {
      fail("Parse error " + ex.toString());
    }
    return mSuccess;
  }
  
  @Override
  public void print(String text) {
    if (mExpectedOutput.size() == 0) {
      fail("Got output \"" + text + "\" when no more was expected.");
      return;
    }
    
    String expected = mExpectedOutput.poll();
    if (!expected.equals(text)) {
      fail("Got output \"" + text + "\", expected \"" + expected + "\"");
    }
  }
  
  private void fail(String message) {
    System.out.println("FAIL " + mPath + ": " + message);
    mSuccess = false;
  }
  
  private final String mPath;
  private final Queue<String> mExpectedOutput = new LinkedList<String>();
  private boolean mSuccess;
}
