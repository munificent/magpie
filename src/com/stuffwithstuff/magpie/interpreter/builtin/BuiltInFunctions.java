package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  /*

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
  */

  @Signature("(this) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createString("<" + arg.getClassObj().getName() + ">");
    }
  }

  @Signature("prints(text String)")
  public static class Print implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      interpreter.print(arg.getTupleField(1).asString());
      return interpreter.nothing();
    }
  }
  
  @Signature("quit()")
  public static class Quit implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      throw new QuitException();
    }
  }
}
