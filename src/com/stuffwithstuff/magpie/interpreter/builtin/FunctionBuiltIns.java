package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.PatternTester;

public class FunctionBuiltIns {
  @Signature("(_ Function) call(arg)")
  public static class Call implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      FnObj function = arg.getTupleField(0).asFn();
      Obj fnArg = arg.getTupleField(1);
      
      // Make sure the argument matches the function's pattern.
      Callable callable = function.getCallable();
      EvalContext context = new EvalContext(callable.getClosure());
      if (!PatternTester.test(interpreter, callable.getPattern(), fnArg,
          context)) {
        throw interpreter.error("NoMethodError");
      }

      return function.invoke(interpreter, fnArg);
    }
  }
}
