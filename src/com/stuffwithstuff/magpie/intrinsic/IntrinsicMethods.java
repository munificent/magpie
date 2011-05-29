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
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringReader;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class IntrinsicMethods {
  @Def("currentTime()")
  public static class CurrentTime implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return context.toObj(time);
    }
  }
  
  @Def("(is Function) call(arg)")
  public static class Call implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FnObj function = left.asFn();
      Obj fnArg = right;
      
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
    public Obj invoke(Context context, Obj left, Obj right) {
      String source = right.asString();
      
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
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asClass().getName());
    }
  }

  @Def("(left) == (right)")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // By default, "==" does reference equality.
      return context.toObj(left == right);
    }
  }

  @Def("(this) toString")
  public static class ToString implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj("<" + left.getClassObj().getName() + ">");
    }
  }
}
