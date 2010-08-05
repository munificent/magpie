package com.stuffwithstuff.magpie;

import java.util.*;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterException;
import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class TestInterpreterHost implements InterpreterHost {
  public TestInterpreterHost(String path) {
    mPath = path;
    mSuccess = true;
  }
  
  public boolean run(String source) {
    try {
      // Parse the expected output.
      int lineNumber = 1;
      for (String line : source.split("\n")) {
        int index = line.indexOf("//:");
        if (index != -1) {
          String expect = line.substring(index + 3).trim();
          mExpectedOutput.add(expect);
        }
        
        if (line.indexOf("//!") != -1) {
          mExpectedErrors.add(lineNumber);
        }

        lineNumber++;
      }
      
      Lexer lexer = new Lexer(mPath, source);
      MagpieParser parser = new MagpieParser(lexer);

      try {
        Interpreter interpreter = new Interpreter(this);
        interpreter.load(parser.parse());
        
        // Do the static analysis and see if we got the errors we expect.
        List<Integer> errors = interpreter.analyze();
        if (mExpectedErrors.size() != errors.size()) {
          fail("Expected " + mExpectedErrors.size() + " errors and got " +
              errors.size() + ".");
        }
        
        for (int i = 0; i < errors.size(); i++) {
          if (mExpectedErrors.get(i) != errors.get(i)) {
            fail("Expected an error on line " + mExpectedErrors.get(i) +
                " and got one on line " + errors.get(i) + " instead.");
          }
        }
        
        interpreter.runMain();
        
        if (mExpectedOutput.size() > 0) {
          fail("Ran out of output when still expecting \"" +
              mExpectedOutput.poll() + "\".");
        }
      } catch (InterpreterException ex) {
        fail("Interpreter error " + ex.toString());
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
  private final List<Integer> mExpectedErrors = new ArrayList<Integer>();
  private boolean mSuccess;
}
