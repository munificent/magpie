package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.ParseException;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  @Signature("import(path String ->)")
  public static class Import_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String currentDir = new File(interpreter.getCurrentScript()).getParent();
      String relativePath = arg.asString();
      File scriptFile = new File(currentDir, relativePath);
      
      try {
        Script script = Script.fromPath(scriptFile.getPath());
        script.execute(interpreter);
      } catch (ParseException e) {
        interpreter.runtimeError(
            "Could not parse script \"%s\".\nError: %s",
            relativePath, e.getMessage());
      } catch (IOException e) {
        interpreter.runtimeError("Could not load script \"%s\"\n%s.",
            relativePath, e);
      }
      
      return interpreter.nothing();
    }
  }

  @Signature("printString(text String ->)")
  public static class PrintRaw implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(arg.asString());
      return interpreter.nothing();
    }
  }

  @Signature("quit(->)")
  public static class Quit implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      throw new QuitException();
    }
  }
  
  @Signature("-(left Int, right Int -> Int)")
  public static class Subtract implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left - right);
    }
  }
  
  @Signature("*(left Int, right Int -> Int)")
  public static class Multiply implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left * right);
    }
  }
  
  @Signature("/(left Int, right Int -> Int)")
  public static class Divide implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left / right);
    }
  }
}
