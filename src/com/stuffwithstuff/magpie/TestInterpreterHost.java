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
    System.out.println(mPath);
    
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

      Lexer lexer = new Lexer(mPath, new StringCharacterReader(source));
      MagpieParser parser = new MagpieParser(lexer);

      try {
        mInterpreter.load(parser.parse());

        // If there is a main() function, then we need to type-check first:
        if (mInterpreter.hasMain()) {
          // Do the static analysis and see if we got the errors we expect.
          Checker checker = new Checker(mInterpreter);
          checker.checkAll();
          List<CheckError> errors = checker.getErrors();
  
          // Go through each error we got.
          for (CheckError error : errors) {
            // Remove it from the collection of errors we expect. We have to
            // search since the errors may not actually be given in order. (The
            // checker is free to check in whatever order it wants.)
            boolean found = false;
            for (int i = 0; i < mExpectedErrors.size(); i++) {
              if (mExpectedErrors.get(i) == error.getLine()) {
                mExpectedErrors.remove(i);
                found = true;
                break;
              }
            }
            
            if (!found) {
              fail("Found an unexpected error on " + error.getPosition() +
                  ": " + error.getMessage());
            }
          }
          
          // We should not have any errors let.
          for (int i = 0; i < mExpectedErrors.size(); i++) {
            fail("Expected an error on line " + mExpectedErrors.get(i)
                + " but got none.");
          }
          
          if (errors.size() == 0) {
            mInterpreter.runMain();
          }
        }
        
        if (mExpectedOutput.size() > 0) {
          fail("Ran out of output when still expecting \""
              + mExpectedOutput.poll().text + "\".");
        }
        
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
