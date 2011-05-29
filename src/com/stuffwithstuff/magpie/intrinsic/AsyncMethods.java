package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Routine;

// TODO(bob): This is all very rough and hacked together.
public class AsyncMethods {
  @Def("run(body is Function)")
  public static class Run implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FnObj function = right.asFn();
      
      Routine routine = new Routine(context, function);
      routine.start();
      
      return context.nothing();
    }
  }

  @Def("sleep(milliseconds is Int)")
  public static class Sleep implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      long milliseconds = right.asInt();
      try {
        Thread.sleep(milliseconds);
      } catch (InterruptedException e) {
        // TODO(bob): Handle error.
        e.printStackTrace();
      }
      
      return context.nothing();
    }
  }
}
