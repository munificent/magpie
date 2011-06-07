package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stuffwithstuff.magpie.util.Expect;

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
  }
  
  public Obj invoke(Context context, Obj left, Obj right) {
    return invoke(context, context.toObj(left, right));
  }
  
  public Obj invoke(Context context, Obj arg) {
    Callable method = select(context, arg);
    
    if (method == null) {
      context.error(Name.NO_METHOD_ERROR, 
          "Could not find a method to match argument " + arg + ".");
    }

    return method.invoke(context, arg);
  }
  
  private Callable select(Context context, Obj arg) {
    Expect.notNull(arg);
    
    List<Callable> applicable = new ArrayList<Callable>();
    for (Callable method : mMethods) {
      // If the callable has a lexical context, evaluate its pattern in that
      // context. That way pattern names can refer to local variables.
      if (PatternTester.test(context, method.getPattern(),
          arg, method.getClosure())) {
        applicable.add(method);
      }
    }
    
    if (applicable.size() == 0) return null;

    linearize(context, applicable);
    
    return applicable.get(0);
  }

  private void linearize(Context context, List<Callable> methods) {
    if (methods.size() <= 1) return;
    Collections.sort(methods, new MethodLinearizer(context));
  }
  
  private final String mDoc;
  private final List<Callable> mMethods = new ArrayList<Callable>();
}
