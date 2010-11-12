package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.ParseException;

public class ObjectBuiltIns {

  @Signature("==(other -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(thisObj == arg);
    }
  }

  @Signature("getField(name String)")
  public static class GetField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      Obj field = thisObj.getField(name);
      if (field == null) return interpreter.nothing();
      
      return field;
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
  
  @Signature("printRaw(text String ->)")
  public static class PrintRaw implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(arg.asString());
      return interpreter.nothing();
    }
  }
  
  @Getter("type(-> Type)")
  public static class Type implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getClassObj();
    }
  }
}
