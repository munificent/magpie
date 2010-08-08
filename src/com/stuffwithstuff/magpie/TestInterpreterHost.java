package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;

import com.stuffwithstuff.magpie.interpreter.CheckError;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterException;
import com.stuffwithstuff.magpie.interpreter.InterpreterHost;

public class TestInterpreterHost implements InterpreterHost {
  public TestInterpreterHost(String path) {
    mPath = path;
    mSuccess = true;
    mInterpreter = new Interpreter(this);
  }

  public boolean run() {
    try {
      // Load the runtime library.
      loadScript("base/base.mag");

      String source = readFile(mPath);

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
        mInterpreter.load(parser.parse());

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

          if (mExpectedOutput.size() > 0) {
            fail("Ran out of output when still expecting \""
                + mExpectedOutput.poll() + "\".");
          }
        }
      } catch (InterpreterException ex) {
        fail("Interpreter error " + ex.toString());
      } catch (UnsupportedOperationException ex) {
        fail("Unsupported operation error " + ex.toString());
      }
    } catch (IOException ex) {
      fail("Couldn't load file: " + ex.toString());
    } catch (ParseException ex) {
      fail("Parse error: " + ex.toString());
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

  private void loadScript(String path) {
    try {
      String source = readFile(path);

      Lexer lexer = new Lexer(path, source);
      MagpieParser parser = new MagpieParser(lexer);

      mInterpreter.load(parser.parse());

    } catch (IOException ex) {
      fail("Couldn't load file: " + ex.toString());
    } catch (ParseException ex) {
      fail("Parse error: " + ex.toString());
    }
  }

  private void fail(String message) {
    System.out.println("FAIL: " + message);
    mSuccess = false;
  }

  private static String readFile(String path) throws IOException {
    FileInputStream stream = new FileInputStream(path);

    try {
      InputStreamReader input = new InputStreamReader(stream, Charset
          .defaultCharset());
      Reader reader = new BufferedReader(input);

      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[8192];
      int read;

      while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
        builder.append(buffer, 0, read);
      }

      return builder.toString();
    } finally {
      stream.close();
    }
  }

  private final String mPath;
  private final Interpreter mInterpreter;
  private final Queue<String> mExpectedOutput = new LinkedList<String>();
  private final List<Integer> mExpectedErrors = new ArrayList<Integer>();
  private boolean mSuccess;
}
