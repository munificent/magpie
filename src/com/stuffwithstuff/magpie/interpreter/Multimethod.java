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
    if (mMethods.contains(method)) return;
    
    mMethods.add(method);
    
    // Push to the other modules that imported this multimethod too.
    for (Multimethod export : mExports) {
      export.addMethod(method);
    }
    
    mSorted = false;
  }
  
  public void addExport(Multimethod multimethod) {
    if (multimethod == this) throw new IllegalArgumentException();
    
    mExports.add(multimethod);
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
    Callable method = mGraph.select(name, context, arg);
    
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
  private List<Multimethod> mExports = new ArrayList<Multimethod>();
}
