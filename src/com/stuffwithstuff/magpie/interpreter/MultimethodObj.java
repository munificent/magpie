package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.util.Expect;

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
  
  public Obj invoke(Interpreter interpreter, Obj receiver, boolean isExplicit, Obj arg) {
    // If we're given a right-hand argument, combine it with the receiver.
    // If not, this is a getter-style multimethod.
    Obj argTuple;
    if (arg != null) {
      argTuple = interpreter.createTuple(receiver, arg);
    } else {
      // The receiver is the entire argument.
      argTuple = receiver;
    }
    FnExpr method = select(interpreter, argTuple);
    
    // If we couldn't find a method on the implicit receiver, see if there's a
    // receiverless method available.
    if ((method == null) && !isExplicit && (arg != null)) {
      argTuple = interpreter.createTuple(interpreter.nothing(), arg);
      method = select(interpreter, argTuple);
    }
    
    if (method == null) {
      interpreter.error("NoMethodError", 
          "Could not find a method to match argument " + argTuple + ".");
    }

    // TODO(bob): In-progress. Once everything is using multimethods, the
    // receiver won't need to be explicitly passed.
    Function function = new Function(interpreter.getGlobals(), null, method);
    return function.invoke(interpreter, receiver, argTuple);
  }
  
  private FnExpr select(Interpreter interpreter, Obj arg) {
    Expect.notNull(arg);
    
    List<FnExpr> applicable = new ArrayList<FnExpr>();
    for (FnExpr method : mMethods) {
      // TODO(bob): Should this be a top level context?
      if (PatternTester.test(interpreter, method.getPattern(), arg,
            interpreter.createTopLevelContext())) {
        applicable.add(method);
      }
    }
    
    if (applicable.size() == 0) return null;

    linearize(interpreter, applicable);
    
    return applicable.get(0);
  }

  private void linearize(Interpreter interpreter, List<FnExpr> methods) {
    if (methods.size() <= 1) return;
    Collections.sort(methods, new MethodLinearizer(interpreter));
  }
  
  private final List<FnExpr> mMethods = new ArrayList<FnExpr>();
}
