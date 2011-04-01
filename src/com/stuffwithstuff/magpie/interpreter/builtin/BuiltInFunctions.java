package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.ParseException;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  @Signature("defineMultimethod(name String, body ->)")
  public static class DefineMethod implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj body = arg.getTupleField(1).asFn();
      
      interpreter.defineMethod(name, body.getCallable());
      
      return interpreter.nothing();
    }
  }

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
        // TODO(bob): Include more information.
        interpreter.error("ParseError");
      } catch (IOException e) {
        // TODO(bob): Include more information.
        interpreter.error("IOError");
      }
      
      return interpreter.nothing();
    }
  }

  @Signature("printString(text String ->)")
  public static class PrintString implements BuiltInCallable {
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
}
