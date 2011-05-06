package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.PatternTester;

public class FunctionBuiltIns {
  @Signature("(is Function) call(arg)")
  public static class Call implements BuiltInCallable {
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
}
