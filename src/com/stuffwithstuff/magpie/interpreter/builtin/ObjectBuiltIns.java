package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.ParseException;

public class ObjectBuiltIns {

  @Signature("==(other Object -> Bool)")
  public static Obj _eqeq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.createBool(thisObj == arg);
  }

  @Signature("getField(name String)")
  public static Obj getField(Interpreter interpreter, Obj thisObj, Obj arg) {
    String name = arg.asString();
    
    Obj field = thisObj.getField(name);
    if (field == null) return interpreter.nothing();
    
    return field;
  }

  @Signature("import(path String ->)")
  public static Obj _import_(Interpreter interpreter, Obj thisObj, Obj arg) {
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
  
  @Signature("printRaw(text String ->)")
  public static Obj printRaw(Interpreter interpreter, Obj thisObj, Obj arg) {
    interpreter.print(arg.asString());
    return interpreter.nothing();
  }
  
  @Getter("type()")
  public static Obj type(Interpreter interpreter, Obj thisObj, Obj arg) {
    return thisObj.getClassObj();
  }
}
