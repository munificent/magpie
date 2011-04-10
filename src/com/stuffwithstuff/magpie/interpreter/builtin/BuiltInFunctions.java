package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.StringCharacterReader;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterHost;
import com.stuffwithstuff.magpie.interpreter.NullInterpreterHost;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  // TODO(bob): This is more or less temp until modules are figured out.
  @Signature("load(path String)")
  public static class Load implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String currentDir = new File(interpreter.getCurrentScript()).getParent();
      String relativePath = arg.getTupleField(1).asString();
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

  // TODO(bob): More or less temporary.
  @Signature("canParse?(source String)")
  public static class CheckSyntax implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String source = arg.getTupleField(1).asString();
      
      boolean canParse = true;
      
      try {
        InterpreterHost host = new NullInterpreterHost();
        Interpreter tempInterpreter = new Interpreter(host);
        Lexer lexer = new Lexer("", new StringCharacterReader(source));
        MagpieParser parser = tempInterpreter.createParser(lexer);

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
