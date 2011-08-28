package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

public class Multimethod {
  public Multimethod(String doc) {
    mDoc = doc;
  }
  
  /**
   * Gets the documentation for the multimethod. This should describe the
   * multimethod in general, while each method's doc describes its behavior
   * for those specific arguments.
   */
  public String getDoc() { return mDoc; }
  
  public List<Callable> getMethods() { return mMethods; }
  
  public void addMethod(Callable method) {
    if (mMethods.contains(method)) {
      throw new IllegalArgumentException("Lame. Shouldn't have dupe methods.");
    }
    
    mMethods.add(method);
    mSorted = false;
  }
  
  public Obj invoke(String name, Context context, Obj left, Obj right) {
    return invoke(name, context, context.toObj(left, right));
  }
  
  public Obj invoke(String name, Context context, Obj arg) {
    if (!mSorted) {
      mGraph.refreshGraph(context, mMethods);
      mSorted = true;
    }
    
    // Select the best method.
    Callable method = mGraph.select(context, arg);
    
    if (method == null) {
      context.error(Name.NO_METHOD_ERROR, 
          "Could not find a method \"" + name + "\" that matches argument " +
          arg + ".");
    }

    return method.invoke(context, arg);
  }
  
  private final String mDoc;
  private boolean mSorted = false;
  private final MethodGraph mGraph = new MethodGraph();
  private List<Callable> mMethods = new ArrayList<Callable>();
}
