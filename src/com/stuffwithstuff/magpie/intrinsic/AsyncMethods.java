package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.Channel;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Routine;

// TODO(bob): This is all very rough and hacked together.
public class AsyncMethods {
  // TODO(bob): Hackish.
  @Def("_setClasses(== Channel)")
  public static class SetClasses implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      sChannelClass = right.asClass();
      
      return context.nothing();
    }
  }
  
  @Def("(== Channel) new(capacity is Int)")
  public static class NewChannel implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      int capacity = right.asInt();
      
      Channel channel = new Channel(capacity);
      return context.instantiate(sChannelClass, channel);
    }
  }
  
  @Def("(is Channel) send(value)")
  public static class Send implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      Channel channel = (Channel) left.getValue();
      
      try {
        channel.send(right);
        return context.nothing();
      } catch (InterruptedException e) {
        // TODO(bob): Better error.
        throw context.error("Error", "Interrupted");
      }
    }
  }
  
  @Def("(is Channel) receive()")
  public static class Receive implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      Channel channel = (Channel) left.getValue();
      
      try {
        return channel.receive();
      } catch (InterruptedException e) {
        // TODO(bob): Better error.
        throw context.error("Error", "Interrupted");
      }
    }
  }

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
  
  private static ClassObj sChannelClass;
}
