package com.stuffwithstuff.magpie;

import java.io.IOException;
import java.util.*;

import com.stuffwithstuff.magpie.interpreter.*;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.Position;

public class TestInterpreterHost implements InterpreterHost {
  public TestInterpreterHost(String path) {
    mPath = path;
    mPassed = true;
    mSkipped = false;
    mInterpreter = new Interpreter(this);
  }
  
  public boolean passed() { return mPassed; }
  public boolean skipped() { return mSkipped; }

  public void run() {
    try {
      // Load the runtime library.
      loadScript("base/base.mag");

      Script script = Script.fromPath(mPath);
      String source = script.getText();

      // Parse the expected output.
      int lineNumber = 1;
      for (String line : source.split("\n")) {
        int index = line.indexOf("//:");
        if (index != -1) {
          String expect = line.substring(index + 3).trim();
          mExpectedOutput.add(new ExpectedOutput(expect, lineNumber));
        }

        if (line.indexOf("//!") != -1) {
          mExpectedErrors.add(lineNumber);
        }

        if (line.indexOf("[disable]") != -1) {
          mPassed = false;
          mSkipped = true;
          return;
        }
        
        lineNumber++;
      }

      Lexer lexer = new Lexer(mPath, source);
      MagpieParser parser = new MagpieParser(lexer);

      try {
        mInterpreter.load(parser.parse());

        /*
        // Do the static analysis and see if we got the errors we expect.
        List<CheckError> errors = mInterpreter.check();
        int count = Math.max(mExpectedErrors.size(), errors.size());
        for (int i = 0; i < count; i++) {
          if (i >= mExpectedErrors.size()) {
            fail("Got an error on line " + errors.get(i).getLine()
                + " when no more were expected: " + errors.get(i));
          } else if (i >= errors.size()) {
            fail("Expected an error on line " + mExpectedErrors.get(i)
                + " but got none.");
          } else if (mExpectedErrors.get(i) != errors.get(i).getLine()) {
            fail("Expected an error on line " + mExpectedErrors.get(i)
                + " but got one on line " + errors.get(i).getLine()
                + " instead: " + errors.get(i));
          }
        }
        
        if (errors.size() == 0) {
          mInterpreter.runMain();
        }
        
        if (mExpectedOutput.size() > 0) {
          fail("Ran out of output when still expecting \""
              + mExpectedOutput.poll() + "\".");
        }
        */
        
      } catch (InterpreterException ex) {
        fail("Interpreter error " + ex.toString());
      } catch (UnsupportedOperationException ex) {
        fail("Unsupported operation error " + ex.toString());
      }
    } catch (IOException ex) {
      fail("Couldn't load file: " + ex.toString());
    } catch (ParseException ex) {
      fail(mPath + ": " + ex.toString());
    }
  }

  private static class ExpectedOutput {
    public ExpectedOutput(String text, int line) {
      this.text = text;
      this.line = line;
    }
    
    public String text;
    public int line;
  }
  
  @Override
  public void print(String text) {
    if (mExpectedOutput.size() == 0) {
      fail("Got output \"" + text + "\" when no more was expected.");
      return;
    }

    ExpectedOutput expected = mExpectedOutput.poll();
    if (!expected.text.equals(text)) {
      fail(String.format("Got output \"%s\" on line %d, expected \"%s\"",
          text, expected.line, expected.text));
    }
  }
  
  public void runtimeError(Position position, String message) {
    // Uncomment this to see the runtime errors as they occur. Commented out
    // because some tests intentionally cause runtime errors to test that the
    // behavior after the error is as expected.
    //System.out.println(position.toString() + ": " + message);
  }

  private void loadScript(String path) {
    try {
      Script script = Script.fromPath(path);
      script.execute(mInterpreter);
    } catch (IOException ex) {
      fail("Couldn't load file: " + ex.toString());
    } catch (ParseException ex) {
      fail("Parse error: " + ex.toString());
    }
  }

  private void fail(String message) {
    System.out.println("FAIL " + mPath + ": " + message);
    mPassed = false;
  }

  private final String mPath;
  private final Interpreter mInterpreter;
  private final Queue<ExpectedOutput> mExpectedOutput = new LinkedList<ExpectedOutput>();
  private final List<Integer> mExpectedErrors = new ArrayList<Integer>();
  private boolean mPassed;
  private boolean mSkipped;
}
