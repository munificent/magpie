package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public class Script {
  public static Script fromPath(String path) throws IOException {
    // If we're given a directory, look for _init.mag in it.
    File filePath = new File(path);
    if (filePath.isDirectory()) {
      path = new File(filePath, "_init.mag").getPath();
    }
    
    return new Script(path, readFile(path));
  }

  public static Script fromString(String text) {
    return new Script("", text);
  }
  
  public static void loadBase(Interpreter interpreter) throws IOException {
    Script script = Script.fromPath("base");
    script.execute(interpreter);
  }
  
  public String getText() { return mText; }

  public void execute() throws IOException {
    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    try {
      // Load the base script first.
      loadBase(interpreter);
      execute(interpreter);
    } catch(ErrorException ex) {
      System.out.println(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()));
    }
  }
  
  public void execute(Interpreter interpreter) {
    interpreter.pushScriptPath(mPath);
    try {
      Lexer lexer = new Lexer(mPath, new StringCharacterReader(mText));
      MagpieParser parser = interpreter.createParser(lexer);
  
      // Evaluate every expression in the file. We do this incrementally so
      // that expressions that define parsers can be used to parse the rest of
      // the file.
      while (true) {
        Expr expr = parser.parseTopLevelExpression();
        if (expr == null) break;
        interpreter.interpret(expr);
      }
      
      // If there is a main() function, then we need to type-check first:
      if (interpreter.hasMain()) {
          interpreter.runMain();
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
