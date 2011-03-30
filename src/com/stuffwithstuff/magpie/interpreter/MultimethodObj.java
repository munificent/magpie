package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stuffwithstuff.magpie.ast.FnExpr;

// TODO(bob): This class is completely hacked together and comically slow. It's
// barely even a proof of concept.
/**
 * Object type for a multimethod object.
 */
public class MultimethodObj extends Obj {
  /**
   * Creates a new MethodObj.
   * 
   * @param classObj     The class of the method object itself: Method.
   */
  public MultimethodObj(ClassObj classObj) {
    super(classObj);
  }
  
  public List<FnExpr> getMethods() { return mMethods; }
  
  public void addMethod(FnExpr method) {
    mMethods.add(method);
  }
  
  public Obj invoke(Interpreter interpreter, Obj receiver, Obj arg) {
    // Combine the receiver and argument into a single object for the method.
    Obj fullArg = interpreter.createTuple(receiver, arg);
    FnExpr method = select(interpreter, fullArg);
    
    // TODO(bob): In-progress. Once everything is using multimethods, the
    // receiver won't need to be explicitly passed.
    Function function = new Function(interpreter.getGlobals(), null, method);
    return function.invoke(interpreter, receiver, fullArg);
  }
  
  private FnExpr select(Interpreter interpreter, Obj arg) {
    List<FnExpr> applicable = new ArrayList<FnExpr>();
    for (FnExpr method : mMethods) {
      // TODO(bob): Should this be a top level context?
      if (PatternTester.test(interpreter, method.getPattern(), arg,
            interpreter.createTopLevelContext())) {
        applicable.add(method);
      }
    }
    
    if (applicable.size() == 0) {
      interpreter.error("NoMethodError", 
          "Could not find a method to match argument " + arg + ".");
    }

    linearize(interpreter, applicable);
    
    return applicable.get(0);
  }

  private void linearize(Interpreter interpreter, List<FnExpr> methods) {
    if (methods.size() <= 1) return;
    Collections.sort(methods, new MethodLinearizer(interpreter));
  }
  
  private final List<FnExpr> mMethods = new ArrayList<FnExpr>();
}
