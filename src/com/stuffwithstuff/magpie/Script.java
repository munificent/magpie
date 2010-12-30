package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import com.stuffwithstuff.magpie.interpreter.CheckError;
import com.stuffwithstuff.magpie.interpreter.Checker;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public class Script {
  public static Script fromPath(String path) throws IOException {
    return new Script(path, readFile(path));
  }

  public static Script fromString(String text) {
    return new Script("", text);
  }
  
  public static void loadBase(Interpreter interpreter) throws IOException {
    Script script = Script.fromPath("base/init.mag");
    script.execute(interpreter);
  }
  
  public String getText() { return mText; }

  public void execute() throws IOException {
    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    // Load the base script first.
    loadBase(interpreter);
    execute(interpreter);
  }
  
  public void execute(Interpreter interpreter) {
    interpreter.pushScriptPath(mPath);
    try {
      Lexer lexer = new Lexer(mPath, new StringCharacterReader(mText));
      MagpieParser parser = new MagpieParser(lexer,
          interpreter.getKeywordParsers());
  
      interpreter.load(parser.parse());
  
      // If there is a main() function, then we need to type-check first:
      if (interpreter.hasMain()) {
        // Do the static analysis and see if we got the errors we expect.
        Checker checker = new Checker(interpreter);
        checker.checkAll();
        List<CheckError> errors = checker.getErrors();
        
        // Show the user any check errors.
        for (CheckError error : errors) {
          System.out.println(error);
        }
        
        // Only run main if there were no errors.
        if (errors.size() == 0) {
          interpreter.runMain();
        }
      }
    } finally {
      interpreter.popScriptPath();
    }
  }

  private static String readFile(String path) throws IOException {
    FileInputStream stream = new FileInputStream(path);

    try {
      InputStreamReader input = new InputStreamReader(
          stream, Charset.forName("UTF-8"));
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

  private Script(String path, String text) {
    mPath = path;
    mText = text;
  }
  
  private final String mPath;
  private final String mText;
}
