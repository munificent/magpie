package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.PatternTester;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringReader;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class IntrinsicMethods {
  @Def("currentTime()")
  public static class CurrentTime implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return context.toObj(time);
    }
  }

  @Def("prints(text is String)")
  public static class Print implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      context.getInterpreter().print(arg.getField(1).asString());
      return context.nothing();
    }
  }
  
  @Def("quit()")
  public static class Quit implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      throw new QuitException();
    }
  }
  
  @Def("(is Function) call(arg)")
  public static class Call implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      FnObj function = arg.getField(0).asFn();
      Obj fnArg = arg.getField(1);
      
      // Make sure the argument matches the function's pattern.
      Callable callable = function.getCallable();
      if (!PatternTester.test(context, callable.getPattern(), fnArg,
          callable.getClosure())) {
        throw context.error(Name.NO_METHOD_ERROR, "The argument \"" +
            context.getInterpreter().evaluateToString(fnArg) + "\" does not match the " +
            "function's pattern " + callable.getPattern());
      }

      return function.invoke(context, fnArg);
    }
  }

  // TODO(bob): More or less temporary.
  @Def("canParse?(source is String)")
  public static class CheckSyntax implements Intrinsic {
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
  
  @Def("(this is Class) name")
  public static class Class_Name implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj(arg.asClass().getName());
    }
  }

  @Def("(left) == (right)")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      // By default, "==" does reference equality.
      return context.toObj(arg.getField(0) == arg.getField(1));
    }
  }

  // TODO(bob): Rename toString.
  @Def("(this) string")
  public static class String_ implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj("<" + arg.getClassObj().getName() + ">");
    }
  }
}
