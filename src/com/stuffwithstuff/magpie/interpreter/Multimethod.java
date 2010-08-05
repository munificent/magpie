package com.stuffwithstuff.magpie.interpreter;


/**
 * Will eventually encapsulate a set of methods with the same name and the logic
 * to select an appropriate based on the signature of the argument.
 */
public class Multimethod {
  public void add(Invokable method) {
    if (mMethod != null) throw new UnsupportedOperationException(
        "Multimethods aren't multi yet :(");
    
    mMethod = method;
  }
  
  public Invokable find(Obj arg) {
    return mMethod;
  }
  
  private Invokable mMethod;
}
