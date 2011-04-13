package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.CharacterReader;

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
    interpreter.interpret(script.getPath(), script.read());
  }
  
  public String getPath() { return mPath; }
  public String getSource() { return mSource; }
  public CharacterReader read() {
    return new StringCharacterReader(mSource);
  }
  
  public void execute() throws IOException {
    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    try {
      // Load the base script first.
      loadBase(interpreter);
      interpreter.interpret(mPath, read());
    } catch(ErrorException ex) {
      System.out.println(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()));
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

  private Script(String path, String source) {
    mPath = path;
    mSource = source;
  }
  
  private final String mPath;
  private final String mSource;
}
