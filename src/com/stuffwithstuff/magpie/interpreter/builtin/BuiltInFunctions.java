package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringReader;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class BuiltInFunctions {
  @Signature("currentTime()")
  public static class CurrentTime implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return context.toObj(time);
    }
  }

  @Signature("prints(text is String)")
  public static class Print implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      context.getInterpreter().print(arg.getField(1).asString());
      return context.nothing();
    }
  }
  
  @Signature("quit()")
  public static class Quit implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      throw new QuitException();
    }
  }

  // TODO(bob): More or less temporary.
  @Signature("canParse?(source is String)")
  public static class CheckSyntax implements BuiltInCallable {
    public Obj invoke(Context context, Obj arg) {
      String source = arg.getField(1).asString();
      
      boolean canParse = true;
      
      try {
        Interpreter tempInterpreter = new Interpreter(context.getInterpreter().getHost());
        MagpieParser parser = MagpieParser.create(
            new StringReader("", source),
            tempInterpreter.getBaseModule().getGrammar());

        while (true) {
          Expr expr = parser.parseTopLevelExpression();
          if (expr == null) break;
        }
      } catch (ParseException e) {
        canParse = false;
      }
      
      return context.toObj(canParse);
    }
  }
}
