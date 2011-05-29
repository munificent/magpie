package com.stuffwithstuff.magpie.interpreter;

// TODO(bob): Pretty ghetto. Should at least use a thread pool.
public class Routine extends Thread {
  public Routine(Context context, FnObj function) {
    mContext = context;
    mFunction = function;
  }
  
  public void run() {
    try {
    mFunction.invoke(mContext, mContext.nothing());
    } catch (ErrorException ex) {
      // TODO(bob): How should this be handled?
      System.out.println(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()));
    }
  }
  
  private final Context mContext;
  private final FnObj mFunction;
}
