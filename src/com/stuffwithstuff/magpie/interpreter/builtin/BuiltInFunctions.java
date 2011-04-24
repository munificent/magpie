package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringCharacterReader;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  @Signature("*importJvmClass*(name String)")
  public static class ImportJvmClass implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String name = arg.getField(1).asString();

      try {
        ClassLoader classLoader = BuiltIns.class.getClassLoader();
        @SuppressWarnings("rawtypes")
        Class javaClass = classLoader.loadClass(name);
        BuiltIns.register(javaClass, interpreter.getCurrentModule().getScope());
      } catch (ClassNotFoundException e) {
        // TODO(bob): Throw better error.
        throw interpreter.error("Error");
      }
      
      return interpreter.nothing();
    }
  }
  
  @Signature("currentTime()")
  public static class CurrentTime implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return interpreter.createInt(time);
    }
  }

  @Signature("prints(text String)")
  public static class Print implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      interpreter.print(arg.getField(1).asString());
      return interpreter.nothing();
    }
  }
  
  @Signature("quit()")
  public static class Quit implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      throw new QuitException();
    }
  }

  // TODO(bob): More or less temporary.
  @Signature("canParse?(source String)")
  public static class CheckSyntax implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String source = arg.getField(1).asString();
      
      boolean canParse = true;
      
      try {
        Interpreter tempInterpreter = new Interpreter(interpreter.getHost());
        MagpieParser parser = tempInterpreter.createParser(
            new StringCharacterReader("", source));

        while (true) {
          Expr expr = parser.parseTopLevelExpression();
          if (expr == null) break;
        }
      } catch (ParseException e) {
        canParse = false;
      }
      
      return interpreter.createBool(canParse);
    }
  }
}
